package id.segari.printer.segariprintermiddleware.service.impl.websocket;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.websocket.WebSocketStatus;
import id.segari.printer.segariprintermiddleware.exception.InternalBaseException;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import id.segari.printer.segariprintermiddleware.service.WebSocketService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class WebSocketServiceImpl implements WebSocketService {
    static final AtomicBoolean isConnecting = new AtomicBoolean(false);
    static final AtomicLong connectedWarehouseId = new AtomicLong(0L);
    static LocalDateTime lastMessageTime;
    static LocalDateTime lastConnectTime;

    private final PrintQueueService printQueueService;
    private final WebSocketStompClient stompClient;
    private final ScheduledExecutorService reconnectExecutor;

    @Value("${websocket.server.url}")
    private String serverUrl;
    @Value("${websocket.topic.print}")
    private String topicDestination;
    @Value("${websocket.reconnect.interval.ms}")
    private long reconnectIntervalMs;

    private StompSession stompSession;

    public WebSocketServiceImpl(PrintQueueService printQueueService, WebSocketStompClient stompClient,
                                ScheduledExecutorService webSocketScheduledExecutor) {
        this.printQueueService = printQueueService;
        this.stompClient = stompClient;
        this.reconnectExecutor = webSocketScheduledExecutor;
    }

    @Override
    public void connect(long warehouseId) {
        validateWebsocketConfig();
        validateWarehouseId(warehouseId);
        validateConnectedWarehouse(warehouseId);
        if (isConnected(warehouseId) || isConnecting.get()) return;
        tryConnect(warehouseId);
    }

    @Override
    public void disconnect(long warehouseId) {
        validateWarehouseId(warehouseId);
        validateDisconnectWarehouse(warehouseId);
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        stompSession = null;
        isConnecting.set(false);
    }

    @Override
    public boolean isConnected(long warehouseId) {
        return stompSession != null && stompSession.isConnected();
    }

    @Override
    public WebSocketStatus getStatus(long warehouseId) {
        return new WebSocketStatus(
                isConnected(warehouseId),
                lastMessageTime,
                lastConnectTime,
                serverUrl
        );
    }

    private void tryConnect(long warehouseId) {
        try {
            if (!createAndConnect(warehouseId).get(5, TimeUnit.SECONDS)){
                throw new InternalBaseException(InternalResponseCode.WEBSOCKET_CONNECTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect to WebSocket");
            }
        } catch (Exception e) {
            throw new InternalBaseException(InternalResponseCode.WEBSOCKET_CONNECTION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect to WebSocket: " + e.getMessage());
        }
    }

    private CompletableFuture<Boolean> createAndConnect(long warehouseId) {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();
        try {
            isConnecting.set(true);
            final StompFrameHandler frameHandler = new PrintStompFrameHandler(printQueueService);
            final StompSessionHandler sessionHandler = getSessionHandler(warehouseId, frameHandler, result);

            stompClient.connectAsync(serverUrl, sessionHandler)
                    .thenAccept(session -> stompSession = session)
                    .exceptionally(_ -> {
                        isConnecting.set(false);
                        result.complete(false);
                        return null;
                    });
        } catch (Exception e) {
            isConnecting.set(false);
            result.complete(false);
        }
        return result;
    }

    private PrintStompSessionHandlerAdapter getSessionHandler(long warehouseId, StompFrameHandler frameHandler,
                                                              CompletableFuture<Boolean> result) {
        return new PrintStompSessionHandlerAdapter(
                warehouseId,
                topicDestination,
                frameHandler,
                reconnectExecutor,
                () -> {
                    if (!isConnected(connectedWarehouseId.get()) && !isConnecting.get()) {
                        createAndConnect(connectedWarehouseId.get());
                    }
                },
                reconnectIntervalMs,
                result
        );
    }

    private void validateConnectedWarehouse(long warehouseId) {
        if (warehouseId != connectedWarehouseId.get() && isConnected(warehouseId)){
            throw new InternalBaseException(InternalResponseCode.WEBSOCKET_ALLOW_ONLY_ONE_WAREHOUSE, HttpStatus.BAD_REQUEST, "warehouse id "+connectedWarehouseId.get()+" is connected. cannot connect other warehouse. please disconnect first.");
        }
    }

    private void validateDisconnectWarehouse(long warehouseId) {
        if (warehouseId != connectedWarehouseId.get()){
            throw new InternalBaseException(InternalResponseCode.WEBSOCKET_WAREHOUSE_NOT_CONNECTED, HttpStatus.BAD_REQUEST, "This warehouse is not connected");
        }
    }

    private void validateWarehouseId(long warehouseId) {
        if (warehouseId <= 0){
            throw new InternalBaseException(InternalResponseCode.INVALID_WAREHOUSE_ID, HttpStatus.BAD_REQUEST, "Ivalid warehouse id");
        }
    }

    private void validateWebsocketConfig() {
        if (serverUrl.isEmpty()) {
            throw new InternalBaseException(InternalResponseCode.WEBSOCKET_EMPTY_CONFIG, HttpStatus.CONFLICT, "Websocket Empty Config");
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            disconnect(connectedWarehouseId.get());
            stompClient.stop();
        } catch (Exception ignored) {}
    }
}