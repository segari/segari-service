package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import id.segari.printer.segariprintermiddleware.service.PrintJobPollingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public SuccessResponse<Map<String, Object>> getPollingStatus() {
        boolean isPolling = printJobPollingService.isPolling();
        int polledJobsCount = printJobPollingService.getPolledJobsCount();

        Map<String, Object> status = Map.of(
                "isPolling", isPolling,
                "polledJobsCount", polledJobsCount
        );

        return new SuccessResponse<>(InternalResponseCode.SUCCESS, status);
    }
}