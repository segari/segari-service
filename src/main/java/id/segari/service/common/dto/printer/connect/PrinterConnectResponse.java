package id.segari.service.common.dto.printer.connect;

import com.fasterxml.jackson.annotation.JsonIgnore;
import id.segari.service.common.InternalResponseCode;

public record PrinterConnectResponse(@JsonIgnore InternalResponseCode code, String status) {
}
