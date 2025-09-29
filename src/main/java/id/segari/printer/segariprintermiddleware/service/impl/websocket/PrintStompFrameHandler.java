package id.segari.printer.segariprintermiddleware.service.impl.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public class PrintStompFrameHandler implements StompFrameHandler {

    private final PrintQueueService printQueueService;
    private final ObjectMapper objectMapper;

    public PrintStompFrameHandler(PrintQueueService printQueueService, ObjectMapper objectMapper) {
        this.printQueueService = printQueueService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return Object.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        try {
            final String jsonString = new String((byte[]) payload);
            final PrinterPrintRequest printRequest = objectMapper.readValue(jsonString, PrinterPrintRequest.class);
            handlePrintRequest(printRequest);
        } catch (Exception _) {}
    }

    private void handlePrintRequest(PrinterPrintRequest printRequest) {
        printQueueService.addToQueue(printRequest);
        WebSocketServiceImpl.lastMessageTime = LocalDateTime.now();
    }
}
