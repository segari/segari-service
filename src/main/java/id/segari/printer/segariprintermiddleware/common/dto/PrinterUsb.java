package id.segari.printer.segariprintermiddleware.common.dto;

public record PrinterUsb(short vendorId, short productId, String productName, String serialNumber, int deviceAddress, int busNumber) {
}
