package id.segari.service.service.impl.fingerprint;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import id.segari.service.common.dto.fingerprint.*;
import id.segari.service.db.entity.FingerprintAdhocUser;
import id.segari.service.db.entity.FingerprintSubject;
import id.segari.service.db.enums.TemplateGroup;
import id.segari.service.db.enums.TemplateVendor;
import id.segari.service.db.repository.FingerprintAdhocUserRepository;
import id.segari.service.db.repository.FingerprintSubjectRepository;
import id.segari.service.exception.BaseException;
import id.segari.service.service.FingerprintExternalService;
import id.segari.service.service.FingerprintService;
import id.segari.service.service.IdentifierService;
import jakarta.annotation.PreDestroy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.usb4java.*;

import java.util.List;
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
    private final AtomicLong connectedWarehouseId = new AtomicLong(0L);
    private final AtomicReference<String> employeeId = new AtomicReference<>(null);
    private final AtomicReference<FingerprintState> state = new AtomicReference<>(FingerprintState.NONE);
    private final AtomicInteger enrollId = new AtomicInteger(1);
    private final AtomicReference<TemplateGroup> enrollTemplateGroup = new AtomicReference<>(null);
    private final byte[][] registerCandidateTemplates = new byte[ENROLLMENT_SCANS_REQUIRED][TEMPLATE_SIZE];

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final FingerprintSubjectRepository fingerprintSubjectRepository;
    private final FingerprintAdhocUserRepository fingerprintAdhocUserRepository;

    private final IdentifierService identifierService;
    private final FingerprintExternalService fingerprintExternalService;
    private Thread workerThread;

    public ZKTecoFingerprintServiceImpl(
            SimpMessagingTemplate simpMessagingTemplate,
            FingerprintSubjectRepository fingerprintSubjectRepository,
            FingerprintAdhocUserRepository fingerprintAdhocUserRepository,
            IdentifierService identifierService,
            FingerprintExternalService fingerprintExternalService
    ) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.fingerprintSubjectRepository = fingerprintSubjectRepository;
        this.fingerprintAdhocUserRepository = fingerprintAdhocUserRepository;
        this.identifierService = identifierService;
        this.fingerprintExternalService = fingerprintExternalService;
    }

    private FingerprintMachine getFingerprintMachine() {
        try {
            final Context context = new Context();
            initContext(context);

            try {
                return findFingerprintDevice(context);
            } finally {
                LibUsb.exit(context);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private FingerprintMachine findFingerprintDevice(final Context context) {
        final DeviceList devices = getDevices(context);
        try {
            return scanDevicesForFingerprint(devices);
        } finally {
            LibUsb.freeDeviceList(devices, true);
        }
    }

    private FingerprintMachine scanDevicesForFingerprint(final DeviceList devices) {
        for (final Device device : devices) {
            final Optional<FingerprintMachine> machine = tryCreateFingerprintMachine(device);
            if (machine.isPresent()) {
                return machine.get();
            }
        }
        throw new BaseException("No fingerprint device found");
    }

    private Optional<FingerprintMachine> tryCreateFingerprintMachine(final Device device) {
        return getDeviceDescriptor(device)
                .filter(descriptor -> isSupportedDevice(descriptor.idVendor()))
                .map(descriptor -> new FingerprintMachine(
                        descriptor.idProduct(),
                        getDeviceName(descriptor.idVendor(), descriptor.idProduct()),
                        getDeviceVendor(descriptor.idVendor())
                ));
    }

    private TemplateVendor getDeviceVendor(final short vendorId) {
        if (isZKT(vendorId)) return TemplateVendor.ZKTECO;
        if (isHID(vendorId)) return TemplateVendor.HID;
        return null;
    }

    private void initContext(final Context context) {
        final int status = LibUsb.init(context);
        if (status != LibUsb.SUCCESS) {
            throw new BaseException("Unable to initialize libusb: " + LibUsb.strError(status));
        }
    }

    private DeviceList getDevices(final Context context) {
        final DeviceList devices = new DeviceList();
        final int status = LibUsb.getDeviceList(context, devices);
        if (status < 0) {
            throw new BaseException("Unable to get device list: " + LibUsb.strError(status));
        }
        return devices;
    }

    private Optional<DeviceDescriptor> getDeviceDescriptor(final Device device) {
        final DeviceDescriptor descriptor = new DeviceDescriptor();
        final int status = LibUsb.getDeviceDescriptor(device, descriptor);
        if (status != LibUsb.SUCCESS) return Optional.empty();
        return Optional.of(descriptor);
    }

    private boolean isZKT(final short vendorId) {
        return vendorId == VENDOR_ID_ZKT;
    }

    private boolean isHID(final short vendorId) {
        return vendorId == VENDOR_ID_HID;
    }

    private String getDeviceName(final short vendorId, final long productId) {
        if (isZKT(vendorId)) {
            return productId == PRODUCT_ID_ZK9500 ? "ZKTeco ZK9500" : "N/A";
        }
        if (isHID(vendorId)) {
            return productId == PRODUCT_ID_UAREU_4500 ? "HID U.are.U 4500" : "N/A";
        }
        return "N/A";
    }

    private boolean isSupportedDevice(final short vendorId) {
        return isZKT(vendorId) || isHID(vendorId);
    }

    private void ensureDeviceNotConnected() {
        if (device.get() != UNINITIALIZED) {
            throw new BaseException("Device already connected");
        }
    }

    private void validatePositive(final int value) {
        if (value < 0) {
            throw new BaseException("No fingerprint device found");
        }
    }

    private void validateNonZero(final long value, final String errorMessage) {
        if (value == 0) {
            throw new BaseException(errorMessage);
        }
    }

    private void sendEnrollmentStatus(final FingerprintEnrollmentStatus status, final int currentScan, final byte[] template) {
        simpMessagingTemplate.convertAndSend(ENROLL_TOPIC,
                new FingerprintEnrollmentResponse(status, ENROLLMENT_SCANS_REQUIRED, currentScan, TemplateVendor.ZKTECO, enrollTemplateGroup.get(), employeeId.get(), template));
    }

    private void sendIdentificationStatus(final FingerprintIdentificationStatus status, final Long userId) {
        simpMessagingTemplate.convertAndSend(IDENTIFY_TOPIC,
                new FingerprintIdentificationResponse(status, userId));
    }

    @Override
    public void connect(long warehouseId) {
        try {
            ensureDeviceNotConnected();
            initializeFingerprintSensor();
            openDevice();
            initializeDatabase();
            loadStoredFingerprintsToMemory();
            resetConnectedState(warehouseId);
            startFingerprintListener();
        } catch (Exception e) {
            freeSensor();
            throw new BaseException(e.getMessage());
        }
    }

    private void resetConnectedState(long warehouseId) {
        connectedWarehouseId.set(warehouseId);
        employeeId.set(null);
        state.set(FingerprintState.NONE);
        enrollId.set(1);
        enrollTemplateGroup.set(null);
    }

    private void loadStoredFingerprintsToMemory() {
        syncWithMemoryDatabase(fingerprintSubjectRepository.findAll());
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

    private void processFingerprintTemplate(final byte[] template) {
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

    private void resetDisconnectedState() {
        connectedWarehouseId.set(0);
        employeeId.set(null);
        state.set(FingerprintState.NONE);
        enrollId.set(1);
        enrollTemplateGroup.set(null);
    }


    @Override
    public void disconnect() {
        freeSensor();
        resetDisconnectedState();
    }

    @PreDestroy
    public void shutdown() {
        disconnect();
    }

    @Override
    public FingerprintStatusResponse getFingerprintStatus() {
        final FingerprintMachine fingerprintMachine = getFingerprintMachine();
        final FingerprintMachineStatus status = getFingerprintMachineStatus(fingerprintMachine);
        return new FingerprintStatusResponse(status, fingerprintMachine, identifierService.get(), connectedWarehouseId.get());
    }

    private FingerprintMachineStatus getFingerprintMachineStatus(final FingerprintMachine fingerprintMachine) {
        if (Objects.isNull(fingerprintMachine)) return FingerprintMachineStatus.UNPLUGGED;
        if (device.get() == 0) return FingerprintMachineStatus.PLUGGED;
        return FingerprintMachineStatus.CONNECTED;
    }

    @Override
    public FingerprintEnrollmentResponse initEnrollment(final String employeeId, final TemplateGroup templateGroup) {
        restart(employeeId, templateGroup);
        return new FingerprintEnrollmentResponse(
                FingerprintEnrollmentStatus.INITIALIZED,
                ENROLLMENT_SCANS_REQUIRED,
                0,
                TemplateVendor.ZKTECO,
                templateGroup,
                employeeId,
                new byte[0]
        );
    }

    @Override
    public FingerprintIdentificationResponse initIdentification() {
        state.set(FingerprintState.IDENTIFICATION);
        return new FingerprintIdentificationResponse(FingerprintIdentificationStatus.INITIALIZED, null);
    }

    @Override
    public void initNone() {
        state.set(FingerprintState.NONE);
    }

    @Override
    @Transactional
    public void sync(final long warehouseId) {
        final List<FingerprintSubjectResponse> responses = fingerprintExternalService.getFingerprintSubject(warehouseId);
        syncFingerprintSubjects(responses);
    }

    @Override
    @Transactional
    public void sync(final long warehouseId, final long internalToolsUserId) {
        final List<FingerprintSubjectResponse> responses = fingerprintExternalService.getFingerprintSubject(warehouseId, internalToolsUserId);
        syncFingerprintSubjects(responses);
    }

    @Override
    @Transactional
    public void add(final String employeeId, boolean adhoc) {
        final List<FingerprintSubjectResponse> responses = fingerprintExternalService.getFingerprintSubject(employeeId);
        if (CollectionUtils.isEmpty(responses)) return;

        if (adhoc) saveAdhocUser(responses.getFirst().internalToolsUserId());
        syncFingerprintSubjects(responses);
    }

    private void saveAdhocUser(final long internalToolsUserId) {
        final FingerprintAdhocUser adhocUser = new FingerprintAdhocUser();
        adhocUser.setInternalToolsUserId(internalToolsUserId);
        fingerprintAdhocUserRepository.save(adhocUser);
    }

    private void syncFingerprintSubjects(final List<FingerprintSubjectResponse> responses) {
        if (CollectionUtils.isEmpty(responses)) return;

        final List<FingerprintSubject> subjects = mapToEntities(responses);
        saveFingerprintSubjects(subjects);
        registerMemoryDatabaseSync(subjects);
    }

    private List<FingerprintSubject> mapToEntities(final List<FingerprintSubjectResponse> responses) {
        return responses.stream()
                .map(response -> new FingerprintSubject(
                        response.id(),
                        response.internalToolsUserId(),
                        response.templateGroup(),
                        response.templateVendor(),
                        response.template()
                ))
                .toList();
    }

    private void saveFingerprintSubjects(final List<FingerprintSubject> subjects) {
        fingerprintSubjectRepository.saveAll(subjects);
    }

    private void registerMemoryDatabaseSync(final List<FingerprintSubject> subjects) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncWithMemoryDatabase(subjects);
            }
        });
    }

    private void syncWithMemoryDatabase(final Iterable<FingerprintSubject> fingerprintSubjects) {
        if (memoryDatabase.get() == 0) return;
        for (final FingerprintSubject fingerprintSubject : fingerprintSubjects) {
            FingerprintSensorEx.DBAdd(memoryDatabase.get(), (int) fingerprintSubject.getId(), fingerprintSubject.getTemplate());
        }
    }

    private void restart(final String employeeId, final TemplateGroup templateGroup) {
        state.set(FingerprintState.ENROLL);
        this.enrollTemplateGroup.set(templateGroup);
        this.employeeId.set(employeeId);
        enrollId.set(0);
    }

    public void handleEnroll(final byte[] template) {
        final int currentScanIndex = enrollId.get();

        if (!isTemplateValid(currentScanIndex, template)) {
            return;
        }

        storeTemplate(currentScanIndex, template);
        final int nextScanIndex = enrollId.incrementAndGet();

        if (needsMoreScans(nextScanIndex)) {
            sendEnrollmentStatus(FingerprintEnrollmentStatus.PARTIALLY_ENROLLED, nextScanIndex, new byte[0]);
            return;
        }

        completeFingerprintEnrollment(nextScanIndex);
    }

    private boolean isTemplateValid(final int currentIndex, final byte[] template) {
        if (currentIndex == 0) {
            return true;
        }
        return FingerprintSensorEx.DBMatch(memoryDatabase.get(),
                registerCandidateTemplates[currentIndex - 1], template) > 0;
    }

    private void storeTemplate(final int index, final byte[] template) {
        System.arraycopy(template, 0, registerCandidateTemplates[index], 0, TEMPLATE_SIZE);
    }

    private boolean needsMoreScans(final int scanIndex) {
        return scanIndex < ENROLLMENT_SCANS_REQUIRED;
    }

    private void completeFingerprintEnrollment(final int finalScanIndex) {
        final byte[] mergedTemplate = mergeEnrollmentTemplates();

        if (mergedTemplate == null) {
            handleEnrollmentFailure();
            return;
        }

        handleEnrollmentSuccess(finalScanIndex, mergedTemplate);
    }

    private byte[] mergeEnrollmentTemplates() {
        final int[] templateLength = new int[]{TEMPLATE_SIZE};
        final byte[] mergedTemplate = new byte[TEMPLATE_SIZE];

        final int mergeResult = FingerprintSensorEx.DBMerge(
                memoryDatabase.get(),
                registerCandidateTemplates[0],
                registerCandidateTemplates[1],
                registerCandidateTemplates[2],
                mergedTemplate,
                templateLength
        );

        final byte[] result = new byte[templateLength[0]];
        System.arraycopy(mergedTemplate, 0, result, 0, result.length);
        return mergeResult == 0 ? result : null;
    }

    private void handleEnrollmentFailure() {
        sendEnrollmentStatus(FingerprintEnrollmentStatus.FAILED, 0, new byte[0]);
        restart(employeeId.get(), enrollTemplateGroup.get());
    }

    private void handleEnrollmentSuccess(final int scanIndex, final byte[] template) {
        sendEnrollmentStatus(FingerprintEnrollmentStatus.ENROLLED, scanIndex, template);
        resetEnrollmentState();
    }

    private void resetEnrollmentState() {
        enrollId.set(0);
        employeeId.set(null);
        state.set(FingerprintState.NONE);
    }

    private void handleIdentify(final byte[] template) {
        try {
            final Integer fingerprintId = identifyFingerprintInDatabase(template);

            if (fingerprintId == null) {
                sendIdentificationStatus(FingerprintIdentificationStatus.NOT_FOUND, null);
                return;
            }

            final Long userId = findUserIdByFingerprintId(fingerprintId);
            sendIdentificationStatus(FingerprintIdentificationStatus.OK, userId);
        } catch (final Exception e) {
            sendIdentificationStatus(FingerprintIdentificationStatus.ERROR, null);
        }
    }

    private Integer identifyFingerprintInDatabase(final byte[] template) {
        final int[] fingerprintId = new int[1];
        final int[] matchScore = new int[1];

        final int result = FingerprintSensorEx.DBIdentify(
                memoryDatabase.get(),
                template,
                fingerprintId,
                matchScore
        );

        return result == 0 ? fingerprintId[0] : null;
    }

    private Long findUserIdByFingerprintId(final Integer fingerprintId) {
        final FingerprintSubject subject = fingerprintSubjectRepository
                .findById(fingerprintId.longValue())
                .orElseThrow(() -> new BaseException("Fingerprint subject not found for ID: " + fingerprintId));

        return subject.getInternalToolsUserId();
    }
}
