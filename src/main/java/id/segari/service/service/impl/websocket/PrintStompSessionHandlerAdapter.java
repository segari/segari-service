package id.segari.service.service.impl.websocket;

import org.springframework.messaging.simp.stomp.*;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static id.segari.service.service.impl.websocket.WebSocketServiceImpl.*;

public class PrintStompSessionHandlerAdapter extends StompSessionHandlerAdapter {
    private final long warehouseId;
    private final String topicDestination;
    private final StompFrameHandler printStompFrameHandler;
    private final ScheduledExecutorService reconnectExecutor;
    private final Runnable onRunReconnect;
    private final long reconnectIntervalMs;
    private final CompletableFuture<Boolean> result;

    public PrintStompSessionHandlerAdapter(long warehouseId,
                                           String topicDestination,
                                           StompFrameHandler printStompFrameHandler,
                                           ScheduledExecutorService reconnectExecutor, Runnable onRunReconnect,
                                           long reconnectIntervalMs, CompletableFuture<Boolean> result) {
        this.warehouseId = warehouseId;
        this.topicDestination = topicDestination;
        this.printStompFrameHandler = printStompFrameHandler;
        this.reconnectExecutor = reconnectExecutor;
        this.onRunReconnect = onRunReconnect;
        this.reconnectIntervalMs = reconnectIntervalMs;
        this.result = result;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        // No - Ops
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        lastConnectTime = LocalDateTime.now();
        isConnecting.set(false);
        try {
            final String topic = topicDestination + "/" + warehouseId;
            session.subscribe(topic, printStompFrameHandler);
            connectedWarehouseId.addAndGet(warehouseId);
            result.complete(true);
        } catch (Exception e) {
            result.complete(false);
        }
    }

    @Override
    public void handleException(StompSession session,
                                StompCommand command,
                                StompHeaders headers,
                                byte[] payload,
                                Throwable exception) {
        isConnecting.set(false);
        scheduleReconnect();
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        isConnecting.set(false);
        scheduleReconnect();
    }

    private void scheduleReconnect() {
        if (connectedWarehouseId.get() == 0) return;
        reconnectExecutor.schedule(onRunReconnect, reconnectIntervalMs, TimeUnit.MILLISECONDS);
    }
}
