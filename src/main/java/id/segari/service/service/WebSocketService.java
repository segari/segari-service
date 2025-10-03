package id.segari.service.service;

import id.segari.service.common.dto.websocket.WebSocketStatus;

public interface WebSocketService {
    void connect(long warehouseId);
    void disconnect(long warehouseId);
    boolean isConnected(long warehouseId);
    WebSocketStatus getStatus(long warehouseId);
}