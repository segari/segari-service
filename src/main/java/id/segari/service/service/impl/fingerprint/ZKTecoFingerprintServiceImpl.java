package id.segari.service.service.impl.fingerprint;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import id.segari.service.common.dto.fingerprint.*;
import id.segari.service.db.entity.FingerprintSubject;
import id.segari.service.db.repository.FingerprintSubjectRepository;
import id.segari.service.exception.BaseException;
import id.segari.service.service.FingerprintService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.usb4java.*;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static id.segari.service.common.dto.fingerprint.FingerprintTopicConstants.ENROLL_TOPIC;
import static id.segari.service.common.dto.fingerprint.FingerprintTopicConstants.IDENTIFY_TOPIC;

@Service
public class ZKTecoFingerprintServiceImpl implements FingerprintService {
    // Constants
    private static final int TEMPLATE_SIZE = 2048;
    private static final int ENROLLMENT_SCANS_REQUIRED = 3;
    private static final short VENDOR_ID_ZKT = 0x1B55;
    private static final short VENDOR_ID_HID = 0x05BA;
    private static final long PRODUCT_ID_ZK9500 = 292;
    private static final long PRODUCT_ID_UAREU_4500 = 10;
    private static final int DB_PARAM_ISO_FORMAT = 5010;
    private static final int UNINITIALIZED = 0;

    // State
    private final AtomicLong device = new AtomicLong(UNINITIALIZED);
    private final AtomicLong memoryDatabase = new AtomicLong(UNINITIALIZED);
    private final AtomicReference<String> employeeId = new AtomicReference<>(null);
    private final AtomicReference<FingerprintState> state = new AtomicReference<>(FingerprintState.NONE);
    private final AtomicInteger enrollId = new AtomicInteger(1);
    private final byte[][] registerCandidateTemplates = new byte[ENROLLMENT_SCANS_REQUIRED][TEMPLATE_SIZE];

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FingerprintSubjectRepository fingerprintSubjectRepository;
    private Thread workerThread;

    public ZKTecoFingerprintServiceImpl(SimpMessagingTemplate simpMessagingTemplate, FingerprintSubjectRepository fingerprintSubjectRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.fingerprintSubjectRepository = fingerprintSubjectRepository;
    }

    @Override
    public FingerprintMachine getFingerprintMachine() {
        final Context context = new Context();
        initContext(context);

        try {
            return findFingerprintDevice(context);
        } finally {
            LibUsb.exit(context);
        }
    }

    private FingerprintMachine findFingerprintDevice(Context context) {
        final DeviceList devices = getDevices(context);
        try {
            return scanDevicesForFingerprint(devices);
        } finally {
            LibUsb.freeDeviceList(devices, true);
        }
    }

    private FingerprintMachine scanDevicesForFingerprint(DeviceList devices) {
        for (Device device : devices) {
            Optional<FingerprintMachine> machine = tryCreateFingerprintMachine(device);
            if (machine.isPresent()) {
                return machine.get();
            }
        }
        throw new BaseException("No fingerprint device found");
    }

    private Optional<FingerprintMachine> tryCreateFingerprintMachine(Device device) {
        return getDeviceDescriptor(device)
                .filter(descriptor -> isSupportedDevice(descriptor.idVendor()))
                .map(descriptor -> new FingerprintMachine(
                        descriptor.idProduct(),
                        getDeviceName(descriptor.idVendor(), descriptor.idProduct())
                ));
    }

    private void initContext(Context context) {
        final int status = LibUsb.init(context);
        if (status != LibUsb.SUCCESS) {
            throw new BaseException("Unable to initialize libusb: " + LibUsb.strError(status));
        }
    }

    private DeviceList getDevices(Context context) {
        final DeviceList devices = new DeviceList();
        final int status = LibUsb.getDeviceList(context, devices);
        if (status < 0) {
            throw new BaseException("Unable to get device list: " + LibUsb.strError(status));
        }
        return devices;
    }

    private Optional<DeviceDescriptor> getDeviceDescriptor(Device device) {
        final DeviceDescriptor descriptor = new DeviceDescriptor();
        final int status = LibUsb.getDeviceDescriptor(device, descriptor);
        if (status != LibUsb.SUCCESS) return Optional.empty();
        return Optional.of(descriptor);
    }

    private boolean isZKT(short vendorId) {
        return vendorId == VENDOR_ID_ZKT;
    }

    private boolean isHID(short vendorId) {
        return vendorId == VENDOR_ID_HID;
    }

    private String getDeviceName(short vendorId, long productId) {
        if (isZKT(vendorId)) {
            return productId == PRODUCT_ID_ZK9500 ? "ZKTeco ZK9500" : "N/A";
        }
        if (isHID(vendorId)) {
            return productId == PRODUCT_ID_UAREU_4500 ? "HID U.are.U 4500" : "N/A";
        }
        return "N/A";
    }

    private boolean isSupportedDevice(short vendorId) {
        return isZKT(vendorId) || isHID(vendorId);
    }

    private void ensureDeviceNotConnected() {
        if (device.get() != UNINITIALIZED) {
            throw new BaseException("Device already connected");
        }
    }

    private void validatePositive(int value) {
        if (value < 0) {
            throw new BaseException("No fingerprint device found");
        }
    }

    private void validateNonZero(long value, String errorMessage) {
        if (value == 0) {
            throw new BaseException(errorMessage);
        }
    }

    private void sendEnrollmentStatus(FingerprintEnrollmentStatus status, int currentScan, byte[] template) {
        simpMessagingTemplate.convertAndSend(ENROLL_TOPIC,
                new FingerprintEnrollmentResponse(status, ENROLLMENT_SCANS_REQUIRED, currentScan, employeeId.get(), template));
    }

    private void sendIdentificationStatus(FingerprintIdentificationStatus status, Long userId) {
        simpMessagingTemplate.convertAndSend(IDENTIFY_TOPIC,
                new FingerprintIdentificationResponse(status, userId));
    }

    @Override
    public void connect() {
        try {
            ensureDeviceNotConnected();
            initializeFingerprintSensor();
            openDevice();
            initializeDatabase();
            startFingerprintListener();
        } catch (Exception e) {
            freeSensor();
            throw new BaseException(e.getMessage());
        }
    }

    private void initializeFingerprintSensor() {
        final int initStatus = FingerprintSensorEx.Init();
        if (initStatus != FingerprintSensorErrorCode.ZKFP_ERR_OK) {
            throw new BaseException("Fingerprint sensor initialization failed");
        }
    }

    private void openDevice() {
        validatePositive(FingerprintSensorEx.GetDeviceCount());
        device.set(FingerprintSensorEx.OpenDevice(0));
        validateNonZero(device.get(), "Failed to open fingerprint device");
    }

    private void initializeDatabase() {
        memoryDatabase.set(FingerprintSensorEx.DBInit());
        validateNonZero(memoryDatabase.get(), "Failed to initialize fingerprint database");
        FingerprintSensorEx.DBSetParameter(memoryDatabase.get(), DB_PARAM_ISO_FORMAT, 1);
    }

    private void startFingerprintListener() {
        workerThread = Thread.ofVirtual().start(new FingerprintListener(device) {
            @Override
            public void onAcquireFingerprint(byte[] template) {
                processFingerprintTemplate(template);
            }
        });
    }

    private void processFingerprintTemplate(byte[] template) {
        switch (state.get()) {
            case ENROLL -> handleEnroll(template);
            case IDENTIFICATION -> handleIdentify(template);
        }
    }

    private void freeSensor() {
        stopWorkerThread();
        freeDatabase();
        closeDevice();
        FingerprintSensorEx.Terminate();
    }

    private void stopWorkerThread() {
        if (Objects.nonNull(workerThread)) {
            workerThread.interrupt();
        }
    }

    private void freeDatabase() {
        if (memoryDatabase.get() != UNINITIALIZED) {
            FingerprintSensorEx.DBFree(memoryDatabase.get());
            memoryDatabase.set(UNINITIALIZED);
        }
    }

    private void closeDevice() {
        if (device.get() != UNINITIALIZED) {
            FingerprintSensorEx.CloseDevice(device.get());
            device.set(UNINITIALIZED);
        }
    }


    @Override
    public void disconnect() {
        freeSensor();
    }

    @Override
    public FingerprintEnrollmentResponse initEnrollment(String employeeId) {
        restart(employeeId);
        return new FingerprintEnrollmentResponse(
                FingerprintEnrollmentStatus.INITIALIZED,
                ENROLLMENT_SCANS_REQUIRED,
                0,
                employeeId,
                new byte[0]
        );
    }

    @Override
    public FingerprintIdentificationResponse initIdentification() {
        state.set(FingerprintState.IDENTIFICATION);
        return new FingerprintIdentificationResponse(FingerprintIdentificationStatus.INITIALIZED, null);
    }

    private void restart(String employeeId) {
        state.set(FingerprintState.ENROLL);
        this.employeeId.set(employeeId);
        enrollId.set(0);
    }

    public void handleEnroll(byte[] template) {
        int currentScanIndex = enrollId.get();

        if (!isTemplateValid(currentScanIndex, template)) {
            return;
        }

        storeTemplate(currentScanIndex, template);
        int nextScanIndex = enrollId.incrementAndGet();

        if (needsMoreScans(nextScanIndex)) {
            sendEnrollmentStatus(FingerprintEnrollmentStatus.PARTIALLY_ENROLLED, nextScanIndex, new byte[0]);
            return;
        }

        completeFingerprintEnrollment(nextScanIndex);
    }

    private boolean isTemplateValid(int currentIndex, byte[] template) {
        if (currentIndex == 0) {
            return true;
        }
        return FingerprintSensorEx.DBMatch(memoryDatabase.get(),
                registerCandidateTemplates[currentIndex - 1], template) > 0;
    }

    private void storeTemplate(int index, byte[] template) {
        System.arraycopy(template, 0, registerCandidateTemplates[index], 0, TEMPLATE_SIZE);
    }

    private boolean needsMoreScans(int scanIndex) {
        return scanIndex < ENROLLMENT_SCANS_REQUIRED;
    }

    private void completeFingerprintEnrollment(int finalScanIndex) {
        byte[] mergedTemplate = mergeEnrollmentTemplates();

        if (mergedTemplate == null) {
            handleEnrollmentFailure();
            return;
        }

        handleEnrollmentSuccess(finalScanIndex, mergedTemplate);
    }

    private byte[] mergeEnrollmentTemplates() {
        int[] templateLength = new int[]{TEMPLATE_SIZE};
        byte[] mergedTemplate = new byte[TEMPLATE_SIZE];

        int mergeResult = FingerprintSensorEx.DBMerge(
                memoryDatabase.get(),
                registerCandidateTemplates[0],
                registerCandidateTemplates[1],
                registerCandidateTemplates[2],
                mergedTemplate,
                templateLength
        );

        return mergeResult == 0 ? mergedTemplate : null;
    }

    private void handleEnrollmentFailure() {
        sendEnrollmentStatus(FingerprintEnrollmentStatus.FAILED, 0, new byte[0]);
        restart(employeeId.get());
    }

    private void handleEnrollmentSuccess(int scanIndex, byte[] template) {
        sendEnrollmentStatus(FingerprintEnrollmentStatus.ENROLLED, scanIndex, template);
        resetEnrollmentState();
    }

    private void resetEnrollmentState() {
        enrollId.set(0);
        employeeId.set(null);
        state.set(FingerprintState.NONE);
    }

    private void handleIdentify(byte[] template) {
        try {
            Integer fingerprintId = identifyFingerprintInDatabase(template);

            if (fingerprintId == null) {
                sendIdentificationStatus(FingerprintIdentificationStatus.NOT_FOUND, null);
                return;
            }

            Long userId = findUserIdByFingerprintId(fingerprintId);
            sendIdentificationStatus(FingerprintIdentificationStatus.Ok, userId);
        } catch (Exception e) {
            sendIdentificationStatus(FingerprintIdentificationStatus.ERROR, null);
        }
    }

    private Integer identifyFingerprintInDatabase(byte[] template) {
        final int[] fingerprintId = new int[1];
        final int[] matchScore = new int[1];

        int result = FingerprintSensorEx.DBIdentify(
                memoryDatabase.get(),
                template,
                fingerprintId,
                matchScore
        );

        return result == 0 ? fingerprintId[0] : null;
    }

    private Long findUserIdByFingerprintId(Integer fingerprintId) {
        FingerprintSubject subject = fingerprintSubjectRepository
                .findById(fingerprintId.longValue())
                .orElseThrow(() -> new BaseException("Fingerprint subject not found for ID: " + fingerprintId));

        return subject.getInternalToolsUserId();
    }
}
