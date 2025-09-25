package id.segari.printer.segariprintermiddleware.common.dto.queue;

public record QueueStatusResponse(int printerId, int queueSize, boolean hasQueue) {
}