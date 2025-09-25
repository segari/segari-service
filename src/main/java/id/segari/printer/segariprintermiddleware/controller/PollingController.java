package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.polling.PollingStatusResponse;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import id.segari.printer.segariprintermiddleware.service.PrintJobPollingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/polling")
public class PollingController {
    private final PrintJobPollingService printJobPollingService;

    public PollingController(PrintJobPollingService printJobPollingService) {
        this.printJobPollingService = printJobPollingService;
    }

    @PostMapping("/start")
    public SuccessResponse<String> startPolling() {
        printJobPollingService.startPolling();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Print job polling started");
    }

    @PostMapping("/stop")
    public SuccessResponse<String> stopPolling() {
        printJobPollingService.stopPolling();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Print job polling stopped");
    }

    @GetMapping("/status")
    public SuccessResponse<PollingStatusResponse> getPollingStatus() {
        boolean isPolling = printJobPollingService.isPolling();
        int polledJobsCount = printJobPollingService.getPolledJobsCount();

        PollingStatusResponse status = new PollingStatusResponse(isPolling, polledJobsCount);

        return new SuccessResponse<>(InternalResponseCode.SUCCESS, status);
    }
}