package id.segari.service.common.dto.queue;

public record QueueStatusResponse(int printerId, int queueSize, boolean hasQueue) {
}