package id.segari.service.common.dto.websocket;

import java.time.LocalDateTime;

public record WebSocketStatus(
        long lastConnectedWarehouseId,
        boolean isConnected,
        LocalDateTime lastMessageTime,
        LocalDateTime lastConnectTime,
        String serverUrl
) {
}