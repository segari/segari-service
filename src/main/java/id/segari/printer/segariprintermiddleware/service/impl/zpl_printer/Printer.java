package id.segari.printer.segariprintermiddleware.service.impl.zpl_printer;

import org.usb4java.Context;
import org.usb4java.DeviceHandle;

public record Printer(short vendorId, short productId, Context context, DeviceHandle deviceHandle) {
}
