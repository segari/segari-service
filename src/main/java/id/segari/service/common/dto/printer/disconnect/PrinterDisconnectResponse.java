package id.segari.service.common.dto.printer.disconnect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import id.segari.service.common.InternalResponseCode;

public record PrinterDisconnectResponse(@JsonIgnore InternalResponseCode code, String status) {
}
