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
        Map<Integer, Integer> queueSizes = printQueueService.getAllQueueSizes();
        int totalQueues = printQueueService.getTotalQueues();
        int totalJobs = queueSizes.values().stream().mapToInt(Integer::intValue).sum();

        QueueOverallStatusResponse status = new QueueOverallStatusResponse(totalQueues, totalJobs, queueSizes);

        return new SuccessResponse<>(InternalResponseCode.SUCCESS, status);
    }

    @GetMapping("/status/{id}")
    public SuccessResponse<QueueStatusResponse> getQueueStatus(@PathVariable int id) {
        int queueSize = printQueueService.getQueueSize(id);

        QueueStatusResponse status = new QueueStatusResponse(id, queueSize, queueSize >= 0);

        return new SuccessResponse<>(InternalResponseCode.SUCCESS, status);
    }

    @GetMapping("/list/{id}")
    public SuccessResponse<List<PrinterPrintRequest>> getPendingJobs(@PathVariable int id) {
        List<PrinterPrintRequest> pendingJobs = printQueueService.getPendingJobs(id);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, pendingJobs);
    }

    @DeleteMapping("/clear/{id}")
    public SuccessResponse<String> clearQueue(@PathVariable int id) {
        boolean cleared = printQueueService.clearQueue(id);
        String message = cleared ?
                "Queue cleared for printer " + id :
                "No queue found for printer " + id;

        return new SuccessResponse<>(InternalResponseCode.SUCCESS, message);
    }

    @DeleteMapping("/clear")
    public SuccessResponse<String> clearAllQueues() {
        printQueueService.clearAllQueues();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, "All queues cleared");
    }
}