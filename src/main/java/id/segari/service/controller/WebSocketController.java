package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.websocket.WebSocketStatus;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.WebSocketService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/websocket")
public class WebSocketController {
    private final WebSocketService webSocketService;

    public WebSocketController(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @PostMapping("/connect/{warehouseId}")
    public SuccessResponse<Boolean> connect(@PathVariable long warehouseId) {
        webSocketService.connect(warehouseId);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @PostMapping("/disconnect/{warehouseId}")
    public SuccessResponse<Boolean> disconnect(@PathVariable long warehouseId) {
        webSocketService.disconnect(warehouseId);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, true);
    }

    @GetMapping("/status/{warehouseId}")
    public SuccessResponse<WebSocketStatus> getStatus(@PathVariable long warehouseId) {
        WebSocketStatus status = webSocketService.getStatus(warehouseId);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, status);
    }
}