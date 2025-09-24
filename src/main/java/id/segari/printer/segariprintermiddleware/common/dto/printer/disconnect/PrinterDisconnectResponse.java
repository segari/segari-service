package id.segari.printer.segariprintermiddleware.common.dto.printer.disconnect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;

public record PrinterDisconnectResponse(@JsonIgnore InternalResponseCode code, String status) {
}
