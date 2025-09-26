package id.segari.printer.segariprintermiddleware.service.impl.websocket;

import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public class PrintStompFrameHandler implements StompFrameHandler {
    private final PrintQueueService printQueueService;

    public PrintStompFrameHandler(PrintQueueService printQueueService) {
        this.printQueueService = printQueueService;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return PrinterPrintRequest.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        if (payload instanceof PrinterPrintRequest printRequest) {
            handlePrintRequest(printRequest);
        }
    }

    private void handlePrintRequest(PrinterPrintRequest printRequest) {
        printQueueService.addToQueue(printRequest);
        WebSocketServiceImpl.lastMessageTime = LocalDateTime.now();
    }
}
