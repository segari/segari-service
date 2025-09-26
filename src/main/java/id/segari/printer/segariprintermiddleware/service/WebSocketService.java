package id.segari.printer.segariprintermiddleware.service;

import id.segari.printer.segariprintermiddleware.common.dto.websocket.WebSocketStatus;

public interface WebSocketService {
    void connect(long warehouseId);
    void disconnect(long warehouseId);
    boolean isConnected(long warehouseId);
    WebSocketStatus getStatus(long warehouseId);
}