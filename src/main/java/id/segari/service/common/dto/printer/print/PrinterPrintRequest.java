package id.segari.service.common.dto.printer.print;

import jakarta.validation.constraints.NotBlank;

public record PrinterPrintRequest(int id,@NotBlank String orderId, @NotBlank String command) {
}
