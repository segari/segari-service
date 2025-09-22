package id.segari.printer.segariprintermiddleware.service.impl;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionBuilder;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import id.segari.printer.segariprintermiddleware.service.ZebraPrinterService;
import org.springframework.stereotype.Service;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ZebraPrinterServiceImpl implements ZebraPrinterService {

    private static final Map<String, Connection> connections = new ConcurrentHashMap<>();

    @Override
    public void connect(String address) {
        try{
            final Connection connection = getConnection(address);
            if (!connection.isConnected()){
                connection.open();
            }
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection getConnection(String address) throws ConnectionException {
        if (connections.containsKey(address)){
            return connections.get(address);
        }
        connections.put(address, ConnectionBuilder.build(address));
        return connections.get(address);
    }

    @Override
    public void disconnect(String address) {
        try{
            final Connection connection = getConnection(address);
            if (connection.isConnected()){
                connection.close();
            }
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void print(String address, String text) {
        try {
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(getConnection(address));
            printer.sendCommand(text);
        } catch (ConnectionException | ZebraPrinterLanguageUnknownException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getUsb() {
        List<Map<String, Object>> usbDevices = new ArrayList<>();

        Context context = new Context();
        int result = LibUsb.init(context);

        if (result != LibUsb.SUCCESS) {
            throw new RuntimeException("Unable to initialize libusb: " + LibUsb.strError(result));
        }

        try {
            DeviceList list = new DeviceList();
            result = LibUsb.getDeviceList(context, list);

            if (result < 0) {
                throw new RuntimeException("Unable to get device list: " + LibUsb.strError(result));
            }

            try {
                for (Device device : list) {
                    DeviceDescriptor descriptor = new DeviceDescriptor();
                    result = LibUsb.getDeviceDescriptor(device, descriptor);

                    if (result == LibUsb.SUCCESS) {
                        Map<String, Object> deviceInfo = new HashMap<>();
                        deviceInfo.put("vendorId", String.format("0x%04X", descriptor.idVendor() & 0xFFFF));
                        deviceInfo.put("productId", String.format("0x%04X", descriptor.idProduct() & 0xFFFF));
                        deviceInfo.put("busNumber", LibUsb.getBusNumber(device));
                        deviceInfo.put("deviceAddress", LibUsb.getDeviceAddress(device));

                        DeviceHandle handle = new DeviceHandle();
                        result = LibUsb.open(device, handle);

                        if (result == LibUsb.SUCCESS) {
                            try {
                                ByteBuffer buffer = ByteBuffer.allocateDirect(256);
                                result = LibUsb.getStringDescriptor(handle, descriptor.iProduct(), (short) 0x0409, buffer);
                                if (result >= 0) {
                                    byte[] bytes = new byte[result];
                                    buffer.get(bytes);
                                    deviceInfo.put("productName", new String(bytes, StandardCharsets.UTF_16LE));
                                }
                            } catch (Exception e) {
                                deviceInfo.put("productName", "Unknown");
                            } finally {
                                LibUsb.close(handle);
                            }
                        } else {
                            deviceInfo.put("productName", "Access denied");
                        }

                        usbDevices.add(deviceInfo);
                    }
                }
            } finally {
                LibUsb.freeDeviceList(list, true);
            }
        } finally {
            LibUsb.exit(context);
        }

        return usbDevices.toString();
    }
}
