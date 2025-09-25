package id.segari.printer.segariprintermiddleware.common.dto.printer.print;

import java.util.List;

public record PrintJobResponse(List<PrinterPrintRequest> printJobs) {
}