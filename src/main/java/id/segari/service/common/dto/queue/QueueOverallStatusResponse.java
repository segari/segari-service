package id.segari.service.common.dto.queue;

import java.util.Map;

public record QueueOverallStatusResponse(int totalQueues, int totalPendingJobs, Map<Integer, Integer> queueSizes) {
}