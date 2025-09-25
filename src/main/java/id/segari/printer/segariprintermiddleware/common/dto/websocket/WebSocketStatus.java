package id.segari.printer.segariprintermiddleware.common.dto.websocket;

import java.time.LocalDateTime;

public record WebSocketStatus(
        boolean isConnected,
        int messagesReceived,
        int reconnectAttempts,
        LocalDateTime lastMessageTime,
        LocalDateTime lastConnectTime,
        String serverUrl
) {
}