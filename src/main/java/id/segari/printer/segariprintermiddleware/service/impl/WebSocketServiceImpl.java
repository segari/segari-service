package id.segari.printer.segariprintermiddleware.service.impl;

import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.common.dto.websocket.WebSocketStatus;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import id.segari.printer.segariprintermiddleware.service.WebSocketService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WebSocketServiceImpl implements WebSocketService {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private final PrintQueueService printQueueService;
    private final WebSocketStompClient stompClient;
    private final ScheduledExecutorService reconnectExecutor;

    @Value("${websocket.server.url:}")
    private String serverUrl;

    @Value("${websocket.topic:/topic/print-jobs}")
    private String topicDestination;

    @Value("${websocket.reconnect.interval.ms:2000}")
    private long reconnectIntervalMs;

    @Value("${websocket.enabled:false}")
    private boolean webSocketEnabled;

    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicInteger messagesReceived = new AtomicInteger(0);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private StompSession stompSession;
    private LocalDateTime lastMessageTime;
    private LocalDateTime lastConnectTime;

    public WebSocketServiceImpl(PrintQueueService printQueueService,
                               WebSocketStompClient stompClient,
                               ScheduledExecutorService webSocketScheduledExecutor) {
        this.printQueueService = printQueueService;
        this.stompClient = stompClient;
        this.reconnectExecutor = webSocketScheduledExecutor;
    }

    @Override
    public void connect() {
        if (!webSocketEnabled || serverUrl.isEmpty()) {
            logger.warn("WebSocket is disabled or no server URL configured");
            return;
        }

        if (isConnected() || isConnecting.get()) {
            logger.warn("WebSocket is already connected or connecting");
            return;
        }

        logger.info("Connecting to STOMP WebSocket server: {}", serverUrl);
        createAndConnect();
    }

    @Override
    public void disconnect() {
        logger.info("Disconnecting from STOMP WebSocket server...");

        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }

        stompSession = null;
        isConnecting.set(false);
        logger.info("STOMP WebSocket disconnected");
    }

    @Override
    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }

    @Override
    public WebSocketStatus getStatus() {
        return new WebSocketStatus(
                isConnected(),
                messagesReceived.get(),
                reconnectAttempts.get(),
                lastMessageTime,
                lastConnectTime,
                serverUrl
        );
    }

    @Override
    public int getMessagesReceived() {
        return messagesReceived.get();
    }

    @Override
    public int getReconnectAttempts() {
        return reconnectAttempts.get();
    }

    private void createAndConnect() {
        try {
            isConnecting.set(true);

            StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    logger.info("STOMP WebSocket connection established");
                    lastConnectTime = LocalDateTime.now();
                    isConnecting.set(false);
                    reconnectAttempts.set(0);

                    // Subscribe to the topic
                    try {
                        session.subscribe(topicDestination, new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return PrinterPrintRequest.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                if (payload instanceof PrinterPrintRequest printRequest) {
                                    handlePrintRequest(printRequest);
                                }
                            }
                        });
                        logger.info("Subscribed to topic: {}", topicDestination);
                    } catch (Exception e) {
                        logger.error("Failed to subscribe to topic {}: {}", topicDestination, e.getMessage());
                    }
                }

                @Override
                public void handleException(StompSession session, StompCommand command,
                                          StompHeaders headers, byte[] payload, Throwable exception) {
                    logger.error("STOMP error: {}", exception.getMessage());
                    isConnecting.set(false);
                    scheduleReconnect();
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    logger.error("STOMP transport error: {}", exception.getMessage());
                    isConnecting.set(false);
                    scheduleReconnect();
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    // Default frame handler - not used in our case
                }
            };

            stompClient.connectAsync(serverUrl, sessionHandler)
                    .thenAccept(session -> {
                        stompSession = session;
                        logger.info("STOMP session established successfully");
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to establish STOMP connection: {}", throwable.getMessage());
                        isConnecting.set(false);
                        scheduleReconnect();
                        return null;
                    });

        } catch (Exception e) {
            logger.error("Failed to create STOMP WebSocket connection: {}", e.getMessage());
            isConnecting.set(false);
            scheduleReconnect();
        }
    }

    private void handlePrintRequest(PrinterPrintRequest printRequest) {
        try {
            logger.debug("Received print request from STOMP: printer={}, command={}",
                        printRequest.id(), printRequest.command());
            lastMessageTime = LocalDateTime.now();
            messagesReceived.incrementAndGet();

            printQueueService.addToQueue(printRequest);
            logger.debug("Added print job to queue for printer {} from STOMP WebSocket", printRequest.id());

        } catch (Exception e) {
            logger.error("Failed to process STOMP print request for printer {}: {}",
                        printRequest.id(), e.getMessage());
        }
    }

    private void scheduleReconnect() {
        if (!webSocketEnabled) {
            return;
        }

        reconnectAttempts.incrementAndGet();
        logger.info("Scheduling WebSocket reconnect attempt #{} in {}ms",
                   reconnectAttempts.get(), reconnectIntervalMs);

        reconnectExecutor.schedule(() -> {
            if (!isConnected() && !isConnecting.get()) {
                logger.info("Attempting to reconnect to WebSocket...");
                createAndConnect();
            }
        }, reconnectIntervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down STOMP WebSocket service...");
        disconnect();

        if (stompClient != null) {
            stompClient.stop();
        }

        logger.info("STOMP WebSocket service shutdown complete");
    }
}