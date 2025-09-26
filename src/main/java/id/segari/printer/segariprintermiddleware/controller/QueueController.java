package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.common.dto.queue.QueueOverallStatusResponse;
import id.segari.printer.segariprintermiddleware.common.dto.queue.QueueStatusResponse;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/queue")
public class QueueController {
    private final PrintQueueService printQueueService;

    public QueueController(PrintQueueService printQueueService) {
        this.printQueueService = printQueueService;
    }

    @GetMapping("/status")
    public SuccessResponse<QueueOverallStatusResponse> getOverallStatus() {
        final Map<Integer, Integer> queueSizes = printQueueService.getAllQueueSizes();
        final int totalQueues = printQueueService.getTotalQueues();
        final int totalJobs = queueSizes.values().stream().mapToInt(Integer::intValue).sum();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, new QueueOverallStatusResponse(totalQueues, totalJobs, queueSizes));
    }

    @GetMapping("/status/{id}")
    public SuccessResponse<QueueStatusResponse> getQueueStatus(@PathVariable int id) {
        int queueSize = printQueueService.getQueueSize(id);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS,  new QueueStatusResponse(id, queueSize, queueSize >= 0));
    }

    @GetMapping("/list/{id}")
    public SuccessResponse<List<PrinterPrintRequest>> getPendingJobs(@PathVariable int id) {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, printQueueService.getPendingJobs(id));
    }

    @DeleteMapping("/clear/{id}")
    public SuccessResponse<Boolean> clearQueue(@PathVariable int id) {
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, printQueueService.clearQueue(id));
    }

    @DeleteMapping("/clear")
    public SuccessResponse<Boolean> clearAllQueues() {
        printQueueService.clearAllQueues();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }
}