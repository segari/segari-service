package id.segari.service.service.impl.zpl_printer;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.PrinterUsb;
import id.segari.service.common.dto.printer.PrinterConnectedResponse;
import id.segari.service.common.dto.printer.connect.PrinterConnectRequest;
import id.segari.service.common.dto.printer.connect.PrinterConnectResponse;
import id.segari.service.common.dto.printer.disconnect.PrinterDisconnectResponse;
import id.segari.service.common.dto.printer.print.PrinterPrintRequest;
import id.segari.service.exception.InternalBaseException;
import id.segari.service.service.PrinterService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ZplPrinterServiceImpl implements PrinterService {
    private static final Logger log = LoggerFactory.getLogger(ZplPrinterServiceImpl.class);
    private static final Map<Integer, Printer> printerById = new ConcurrentHashMap<>();
    private static final Map<Integer, ReentrantLock> lockById = new ConcurrentHashMap<>();

    @Override
    public PrinterConnectResponse connect(PrinterConnectRequest request) {
        final ReentrantLock lock = getLock(request.id());
        lock.lock();
        try {
            if (printerById.containsKey(request.id())) {
                return new PrinterConnectResponse(InternalResponseCode.PRINTER_ALREADY_CONNECTED, "Printer Already Connected");
            }
            final Printer printer = openUsbDevice(request);
            printerById.put(request.id(), printer);
            log.info("Printer connected: id={}, vendorId={}, productId={}",
                request.id(), request.vendorId(), request.productId());
            return new PrinterConnectResponse(InternalResponseCode.SUCCESS_CONNECTING_PRINTER, "Success Connecting Printer");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public PrinterDisconnectResponse disconnect(int id) {
        final ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            if (!printerById.containsKey(id)) {
                return new PrinterDisconnectResponse(InternalResponseCode.CANNOT_FIND_CONNECTED_PRINTER, "Cannot Find Connected Printer");
            }

            final Printer printer = printerById.remove(id);
            cleanupPrinterResources(printer, true); // Throw on error for explicit disconnect
            removeLock(id);
            log.info("Printer disconnected: id={}", id);
            return new PrinterDisconnectResponse(InternalResponseCode.SUCCESS_DISCONNECTING_PRINTER, "Success Disconnecting Printer");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void print(PrinterPrintRequest request) {
        final ReentrantLock lock = getLock(request.id());
        lock.lock();
        try {
            if (!printerById.containsKey(request.id())) {
                throw new InternalBaseException(InternalResponseCode.CANNOT_FIND_CONNECTED_PRINTER, HttpStatus.BAD_REQUEST, "Cannot find connected printer with id: " + request.id());
            }
            final Printer printer = printerById.get(request.id());
            final ByteBuffer buffer = getByteBuffer(request);
            final IntBuffer transferred = IntBuffer.allocate(1);
            final int status = LibUsb.bulkTransfer(printer.deviceHandle(), (byte) 0x01, buffer, transferred, 5000); // 0x01 is hardcoded. use findPrinterEndpoint later
            if (status != LibUsb.SUCCESS) {
                throw new InternalBaseException(InternalResponseCode.FAILED_TO_PRINT, HttpStatus.CONFLICT, "Failed to print");
            }
            log.debug("Print successful: id={}, bytes transferred={}", request.id(), transferred.get(0));
        } finally {
            lock.unlock();
        }
    }

    public List<PrinterUsb> getConnectedUsb() {
        final Context context = new Context();
        initContext(context);
        return getAllConnectedPrinter(context);
    }

    public List<PrinterUsb> getPluggedUsb() {
        final Context context = new Context();
        initContext(context);
        return getAllPluggedPrinter(context);
    }

    private ByteBuffer getByteBuffer(PrinterPrintRequest request) {
        final byte[] data = request.command().getBytes(StandardCharsets.UTF_8);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);
        buffer.rewind();
        return buffer;
    }

    @Override
    public List<PrinterUsb> getAllPrinter() {
        final List<PrinterUsb> connectedPrinter = getConnectedUsb();
        final List<PrinterUsb> pluggedPrinter = getPluggedUsb();
        connectedPrinter.addAll(pluggedPrinter);
        return connectedPrinter;
    }

    private List<PrinterUsb> getAllConnectedPrinter(Context context) {
        try {
            final DeviceList devices = getDevices(context);
            return convertConnected(devices);
        }finally {
            LibUsb.exit(context);
        }
    }

    private List<PrinterUsb> getAllPluggedPrinter(Context context) {
        try {
            final DeviceList devices = getDevices(context);
            return convertPlugged(devices);
        }finally {
            LibUsb.exit(context);
        }
    }

    private List<PrinterUsb> convertConnected(DeviceList devices){
        final List<PrinterUsb> result = new LinkedList<>();
        try {
            final Set<String> scannedSerialNumber = new HashSet<>();
            for (Printer printer : printerById.values()) {
                for (Device device : devices) {
                    final Optional<DeviceDescriptor> descriptorOptional = getDeviceDescriptor(device);
                    if (descriptorOptional.isPresent()) {
                        final DeviceDescriptor descriptor = descriptorOptional.get();
                        if (descriptor.bDeviceClass() == 7 || isPrinterVendor(descriptor.idVendor())){
                            if (descriptor.idVendor() == printer.vendorId() && descriptor.idProduct() == printer.productId()) {
                                final DeviceHandle testHandle = new DeviceHandle();
                                final int status = LibUsb.open(device, testHandle);
                                final String serialNumber = getSerialNumber(descriptor, printer.deviceHandle());
                                if (status != LibUsb.SUCCESS) {
                                    if (scannedSerialNumber.contains(serialNumber)) continue;
                                    result.add(new PrinterUsb(
                                            printer.vendorId(),
                                            printer.productId(),
                                            getProductName(descriptor, printer.deviceHandle()),
                                            serialNumber
                                    ));
                                    scannedSerialNumber.add(serialNumber);
                                    break;
                                } else {
                                    LibUsb.close(testHandle);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            LibUsb.freeDeviceList(devices, true);
        }
        return result;
    }

    private List<PrinterUsb> convertPlugged(DeviceList devices){
        try {
           final List<PrinterUsb> printers = new LinkedList<>();
            for (final Device device : devices) {
                try {
                    final Optional<DeviceDescriptor> descriptorOptional = getDeviceDescriptor(device);
                    if (descriptorOptional.isPresent()){
                        final DeviceDescriptor descriptor = descriptorOptional.get();
                        if (descriptor.bDeviceClass() == 7 || isPrinterVendor(descriptor.idVendor())){
                            final PrinterDescription printerDescription = getPrinterDescription(device, descriptor);
                            final PrinterUsb printer = new PrinterUsb(
                                    descriptor.idVendor(),
                                    descriptor.idProduct(),
                                    printerDescription.productName(),
                                    printerDescription.serialNumber()
                                    );
                            printers.add(printer);
                        }
                    }
                } catch (Exception _) {}
            }
           return printers;
        }finally {
            LibUsb.freeDeviceList(devices, true);
        }
    }

    private PrinterDescription getPrinterDescription(Device device, DeviceDescriptor descriptor) {
        final DeviceHandle deviceHandle = new DeviceHandle();
        final int status = LibUsb.open(device, deviceHandle);
        if (status != LibUsb.SUCCESS) throw new RuntimeException("failed to open device handler");
        try {
            return new PrinterDescription(
                    getProductName(descriptor, deviceHandle),
                    getSerialNumber(descriptor, deviceHandle)
                    );
        } catch (Exception e) {
            return new PrinterDescription(getUnknown(), getUnknown());
        }finally {
            LibUsb.close(deviceHandle);
        }
    }

    private String getSerialNumber(DeviceDescriptor descriptor, DeviceHandle deviceHandle) {
        return LibUsb.getStringDescriptor(deviceHandle, descriptor.iSerialNumber());
    }

    private String getUnknown() {
        return "UNKNOWN";
    }

    private String getProductName(DeviceDescriptor descriptor, DeviceHandle deviceHandle) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(256);
        final int status = LibUsb.getStringDescriptor(deviceHandle, descriptor.iProduct(), (short) 0x0409, buffer);
        if (status < 0) return getUnknown();
        final byte[] bytes = new byte[status];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }

    private boolean isPrinterVendor(short vendorId) {
        return vendorId == 0x0A5F || // Zebra
                vendorId == 0x2D37;   // XPrinter;
    }

    private Optional<DeviceDescriptor> getDeviceDescriptor(Device device){
        final DeviceDescriptor descriptor = new DeviceDescriptor();
        final int status = LibUsb.getDeviceDescriptor(device, descriptor);
        if (status != LibUsb.SUCCESS) return Optional.empty();
        return Optional.of(descriptor);
    }

    private DeviceList getDevices(Context context) {
        final DeviceList devices = new DeviceList();
        final int status = LibUsb.getDeviceList(context, devices);
        if (status < 0){
            throw new InternalBaseException(InternalResponseCode.UNABLE_TO_GET_DEVICE_LIST, HttpStatus.CONFLICT, "Unable to get device list: " + LibUsb.strError(status));
        }
        return devices;
    }

    @Override
    public List<PrinterConnectedResponse> getConnected() {
        return List.of();
    }

    @Override
    public boolean isConnected(int id) {
        final ReentrantLock lock = getLock(id);
        lock.lock();
        try {
            // Quick check: if not in map, return false
            if (!printerById.containsKey(id)) {
                return false;
            }

            final Printer printer = printerById.get(id);

            // Verify device is still physically connected
            if (!verifyDeviceStillConnected(printer)) {
                log.warn("Printer id={} is no longer physically connected. Auto-removing from connected printers.", id);

                // Cleanup resources (don't throw on error since device is already gone)
                cleanupPrinterResources(printer, false);

                // Remove from map
                printerById.remove(id);

                return false;
            }

            return true;
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock getLock(int id) {
        return lockById.computeIfAbsent(id, k -> new ReentrantLock());
    }

    private void removeLock(int id) {
        lockById.remove(id);
    }

    private void cleanupPrinterResources(Printer printer, boolean throwOnError) {
        final Context context = printer.context();
        final DeviceHandle deviceHandle = printer.deviceHandle();

        // Try to release interface
        try {
            final int releaseStatus = LibUsb.releaseInterface(deviceHandle, 0);
            if (releaseStatus != LibUsb.SUCCESS) {
                final String error = "Failed to release interface: " + LibUsb.strError(releaseStatus);
                if (throwOnError) {
                    throw new InternalBaseException(InternalResponseCode.INTERNAL_ERROR, HttpStatus.CONFLICT, error);
                } else {
                    log.warn(error);
                }
            }
        } catch (Exception e) {
            if (throwOnError) {
                throw e;
            } else {
                log.warn("Exception while releasing interface: {}", e.getMessage());
            }
        }

        // Try to close device handle
        try {
            LibUsb.close(deviceHandle);
        } catch (Exception e) {
            if (throwOnError) {
                throw e;
            } else {
                log.warn("Exception while closing device handle: {}", e.getMessage());
            }
        }

        // Try to exit context
        try {
            LibUsb.exit(context);
        } catch (Exception e) {
            if (throwOnError) {
                throw e;
            } else {
                log.warn("Exception while exiting context: {}", e.getMessage());
            }
        }
    }

    private boolean verifyDeviceStillConnected(Printer printer) {
        try {
            // Try to get the device from the existing handle
            final Device device = LibUsb.getDevice(printer.deviceHandle());
            if (device == null) {
                log.debug("Device handle no longer has an associated device");
                return false;
            }

            // Try to get device descriptor to verify the device is still accessible
            final DeviceDescriptor descriptor = new DeviceDescriptor();
            final int status = LibUsb.getDeviceDescriptor(device, descriptor);

            if (status != LibUsb.SUCCESS) {
                log.debug("Failed to get device descriptor: {}", LibUsb.strError(status));
                return false;
            }

            // Verify vendorId and productId match (sanity check)
            if (descriptor.idVendor() != printer.vendorId() ||
                descriptor.idProduct() != printer.productId()) {
                log.warn("Device vendorId/productId mismatch - expected {}/{}, got {}/{}",
                    printer.vendorId(), printer.productId(),
                    descriptor.idVendor(), descriptor.idProduct());
                return false;
            }

            // Try a simple operation on the existing handle to verify it's still valid
            // Attempting to get a string descriptor - if device is unplugged, this will fail
            try {
                final String serial = getSerialNumber(descriptor, printer.deviceHandle());
                // If we can read the serial number, the device is still connected
                return serial != null && !serial.isEmpty();
            } catch (Exception e) {
                log.debug("Failed to read serial number from device handle: {}", e.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.debug("Exception while verifying device connection: {}", e.getMessage());
            return false;
        }
    }

    private Printer openUsbDevice(PrinterConnectRequest request) {
        final Context context = new Context();
        initContext(context);
        final DeviceHandle deviceHandle = getDeviceHandle(request, context);
        detachKernelDriverIfActive(context, deviceHandle);
        claimInterface(context, deviceHandle);
        return new Printer(request.vendorId(), request.productId(), context, deviceHandle);
    }

    void claimInterface(Context context, DeviceHandle deviceHandle){
        final int status = LibUsb.claimInterface(deviceHandle, 0);
        if (status != LibUsb.SUCCESS){
            LibUsb.close(deviceHandle);
            LibUsb.exit(context);
            throw new InternalBaseException(InternalResponseCode.UNABLE_TO_CLAIM_INTERFACE, HttpStatus.CONFLICT, "Unable to claim interface: " + LibUsb.strError(status));
        }
    }

    private void detachKernelDriverIfActive(Context context, DeviceHandle deviceHandle) {
        if (LibUsb.kernelDriverActive(deviceHandle, 0) != 1) return;
        final int status = LibUsb.detachKernelDriver(deviceHandle, 0);
        if (status != LibUsb.SUCCESS && status != LibUsb.ERROR_NOT_FOUND && status != LibUsb.ERROR_NOT_SUPPORTED){
            LibUsb.close(deviceHandle);
            LibUsb.exit(context);
            throw new InternalBaseException(InternalResponseCode.UNABLE_TO_DETACH_KERNEL_DRIVER, HttpStatus.CONFLICT, "Unable to detach kernel driver: " + LibUsb.strError(status));
        }
    }

    private DeviceHandle getDeviceHandle(PrinterConnectRequest request, Context context) {
        final DeviceHandle deviceHandle = getDeviceHandleBySerial(request, context);
        if (deviceHandle == null){
            LibUsb.exit(context);
            throw new InternalBaseException(InternalResponseCode.USB_DEVICE_NOT_FOUND, HttpStatus.BAD_REQUEST, "USB device not found: vendorId="+request.vendorId()+", productId="+request.productId());
        }
        return deviceHandle;
    }

    @Nullable
    private DeviceHandle getDeviceHandleBySerial(PrinterConnectRequest request, Context context) {
        final DeviceList list = new DeviceList();
        LibUsb.getDeviceList(context, list);

        try {
            for (Device device : list) {
                final DeviceDescriptor descriptor = new DeviceDescriptor();
                LibUsb.getDeviceDescriptor(device, descriptor);

                if (descriptor.idVendor() == request.vendorId() && descriptor.idProduct() == request.productId()) {
                    final DeviceHandle handle = new DeviceHandle();
                    int result = LibUsb.open(device, handle);

                    if (result == LibUsb.SUCCESS) {
                        // Get serial number
                        final String serial = LibUsb.getStringDescriptor(handle, descriptor.iSerialNumber());

                        if (request.serialNumber().equals(serial)) {
                            return handle;
                        } else {
                            LibUsb.close(handle);
                        }
                    }
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }
        return null;
    }

    private void initContext(Context context) {
        final int status = LibUsb.init(context);
        if (status != LibUsb.SUCCESS){
            throw new InternalBaseException(InternalResponseCode.INIT_CONTEXT_FAILED, HttpStatus.CONFLICT, "Unable to initialize libusb: " + LibUsb.strError(status));
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ZplPrinterService. Cleaning up {} connected printers.", printerById.size());

        // Acquire all locks to prevent concurrent operations during shutdown
        final List<ReentrantLock> acquiredLocks = new ArrayList<>();
        for (Integer id : lockById.keySet()) {
            final ReentrantLock lock = getLock(id);
            lock.lock();
            acquiredLocks.add(lock);
        }

        try {
            // Cleanup all printers
            for (Map.Entry<Integer, Printer> entry : printerById.entrySet()) {
                try {
                    log.debug("Cleaning up printer id={}", entry.getKey());
                    cleanupPrinterResources(entry.getValue(), false);
                } catch (Exception e) {
                    log.error("Error cleaning up printer id={}: {}", entry.getKey(), e.getMessage());
                }
            }

            // Clear maps
            printerById.clear();
            lockById.clear();

            log.info("ZplPrinterService shutdown complete.");
        } finally {
            // Release all locks
            for (ReentrantLock lock : acquiredLocks) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.warn("Error releasing lock during shutdown: {}", e.getMessage());
                }
            }
        }
    }
}
