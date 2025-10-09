package id.segari.service.common.dto;

public record PrinterUsb(short vendorId, short productId, String productName, String serialNumber) {
}
