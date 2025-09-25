package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.websocket.WebSocketStatus;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import id.segari.printer.segariprintermiddleware.service.WebSocketService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/websocket")
public class WebSocketController {
    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @PostMapping("/connect")
    public SuccessResponse<String> connect() {
        try {
            webSocketService.connect();
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "WebSocket connection initiated");
        } catch (Exception e) {
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Connection failed: " + e.getMessage());
        }
    }

    @PostMapping("/disconnect")
    public SuccessResponse<String> disconnect() {
        try {
            webSocketService.disconnect();
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "WebSocket disconnected successfully");
        } catch (Exception e) {
            return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Disconnect failed: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    public SuccessResponse<WebSocketStatus> getStatus() {
        WebSocketStatus status = webSocketService.getStatus();
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, status);
    }
}