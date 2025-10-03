package id.segari.service.common.dto.printer.print;

import java.util.List;

public record PrintJobResponse(List<PrinterPrintRequest> printJobs) {
}