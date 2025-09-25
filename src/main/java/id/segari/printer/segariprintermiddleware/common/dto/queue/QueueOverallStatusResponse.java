package id.segari.printer.segariprintermiddleware.common.dto.queue;

import java.util.Map;

public record QueueOverallStatusResponse(int totalQueues, int totalPendingJobs, Map<Integer, Integer> queueSizes) {
}