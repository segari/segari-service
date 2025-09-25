package id.segari.printer.segariprintermiddleware.common.dto.polling;

public record PollingStatusResponse(boolean isPolling, int polledJobsCount) {
}