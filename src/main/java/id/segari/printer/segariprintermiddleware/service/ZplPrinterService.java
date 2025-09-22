package id.segari.printer.segariprintermiddleware.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ZplPrinterService {

    private static final Map<String, DeviceHandle> usbConnections = new ConcurrentHashMap<>();

    /**
     * Print ZPL commands to USB printer
     */
    public boolean printToUSB(int vendorId, int productId, String zplCommands) {
        String connectionKey = vendorId + ":" + productId;

        try {
            DeviceHandle handle = usbConnections.get(connectionKey);

            if (handle == null) {
                handle = openUSBDevice(vendorId, productId);
                if (handle == null) {
                    return false;
                }
                usbConnections.put(connectionKey, handle);
            }

            byte[] data = zplCommands.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
            buffer.put(data);
            buffer.rewind();

            // Assuming endpoint 0x01 for output (this may vary by printer)
            IntBuffer transferred = IntBuffer.allocate(1);
            int result = LibUsb.bulkTransfer(handle, (byte) 0x01, buffer, transferred, 5000);

            if (result != LibUsb.SUCCESS) {
                log.error("USB transfer failed: {}", LibUsb.strError(result));
                return false;
            }

            log.debug("Transferred {} bytes to USB printer", transferred.get(0));

            log.info("ZPL commands sent to USB printer: {}", connectionKey);
            return true;

        } catch (Exception e) {
            log.error("Failed to print to USB printer {}: {}", connectionKey, e.getMessage());
            return false;
        }
    }

    /**
     * Open USB device connection
     */
    private DeviceHandle openUSBDevice(int vendorId, int productId) {
        Context context = new Context();
        int result = LibUsb.init(context);

        if (result != LibUsb.SUCCESS) {
            log.error("Unable to initialize libusb: {}", LibUsb.strError(result));
            return null;
        }

        try {
            DeviceHandle handle = LibUsb.openDeviceWithVidPid(context, (short) vendorId, (short) productId);

            if (handle == null) {
                log.error("USB device not found: vendorId={}, productId={}", vendorId, productId);
                return null;
            }

            // Detach kernel driver if active
            if (LibUsb.kernelDriverActive(handle, 0) == 1) {
                LibUsb.detachKernelDriver(handle, 0);
            }

            // Claim interface
            result = LibUsb.claimInterface(handle, 0);
            if (result != LibUsb.SUCCESS) {
                log.error("Unable to claim interface: {}", LibUsb.strError(result));
                LibUsb.close(handle);
                return null;
            }

            log.info("USB device opened successfully: vendorId={}, productId={}", vendorId, productId);
            return handle;

        } finally {
            LibUsb.exit(context);
        }
    }

    /**
     * List available USB printers
     */
    public List<Map<String, Object>> listUSBPrinters() {
        List<Map<String, Object>> printers = new ArrayList<>();
        Context context = new Context();
        int result = LibUsb.init(context);

        if (result != LibUsb.SUCCESS) {
            log.error("Unable to initialize libusb: {}", LibUsb.strError(result));
            return printers;
        }

        try {
            DeviceList list = new DeviceList();
            result = LibUsb.getDeviceList(context, list);

            if (result < 0) {
                log.error("Unable to get device list: {}", LibUsb.strError(result));
                return printers;
            }

            try {
                for (Device device : list) {
                    DeviceDescriptor descriptor = new DeviceDescriptor();
                    result = LibUsb.getDeviceDescriptor(device, descriptor);

                    if (result == LibUsb.SUCCESS) {
                        // Check if it's likely a printer (class 7 = printer)
                        if (descriptor.bDeviceClass() == 7 || isPrinterVendor(descriptor.idVendor() & 0xFFFF)) {
                            Map<String, Object> printerInfo = new ConcurrentHashMap<>();
                            printerInfo.put("vendorId", descriptor.idVendor() & 0xFFFF);
                            printerInfo.put("productId", descriptor.idProduct() & 0xFFFF);
                            printerInfo.put("busNumber", LibUsb.getBusNumber(device));
                            printerInfo.put("deviceAddress", LibUsb.getDeviceAddress(device));
                            printers.add(printerInfo);
                        }
                    }
                }
            } finally {
                LibUsb.freeDeviceList(list, true);
            }
        } finally {
            LibUsb.exit(context);
        }

        return printers;
    }

    /**
     * Check if vendor ID is likely a printer manufacturer
     */
    private boolean isPrinterVendor(int vendorId) {
        // Common printer vendor IDs
        return vendorId == 0x0A5F || // Zebra
               vendorId == 0x04B8 || // Epson
               vendorId == 0x03F0 || // HP
               vendorId == 0x04A9 || // Canon
               vendorId == 0x067B || // Prolific (USB-to-serial adapters)
               vendorId == 0x0483;   // STMicroelectronics (some printers)
    }


    /**
     * Close USB connection
     */
    public void closeUSBConnection(int vendorId, int productId) {
        String connectionKey = vendorId + ":" + productId;
        DeviceHandle handle = usbConnections.remove(connectionKey);

        if (handle != null) {
            LibUsb.releaseInterface(handle, 0);
            LibUsb.close(handle);
            log.info("Closed USB connection: {}", connectionKey);
        }
    }

    /**
     * Close all USB connections
     */
    public void closeAllUSBConnections() {
        // Close USB connections
        usbConnections.forEach((key, handle) -> {
            LibUsb.releaseInterface(handle, 0);
            LibUsb.close(handle);
        });
        usbConnections.clear();

        log.info("All USB printer connections closed");
    }
}