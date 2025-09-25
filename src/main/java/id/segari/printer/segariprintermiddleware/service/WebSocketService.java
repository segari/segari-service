package id.segari.printer.segariprintermiddleware.service;

import id.segari.printer.segariprintermiddleware.common.dto.websocket.WebSocketStatus;

public interface WebSocketService {
    void connect();

    void disconnect();

    boolean isConnected();

    WebSocketStatus getStatus();

    int getMessagesReceived();

    int getReconnectAttempts();
}