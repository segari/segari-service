package id.segari.printer.segariprintermiddleware.common.dto.printer.connect;

import jakarta.validation.constraints.NotBlank;

public record PrinterConnectRequest(int id, short vendorId, short productId, @NotBlank String serialNumber) {
}
