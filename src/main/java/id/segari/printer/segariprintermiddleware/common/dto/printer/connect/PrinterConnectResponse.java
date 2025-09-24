package id.segari.printer.segariprintermiddleware.common.dto.printer.connect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;

public record PrinterConnectResponse(@JsonIgnore InternalResponseCode code, String status) {
}
