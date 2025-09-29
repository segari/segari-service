package id.segari.printer.segariprintermiddleware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class WebSocketConfig {

    @Bean
    public WebSocketStompClient webSocketStompClient() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        // Configure task scheduler
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadFactory(Thread.ofVirtual().name("vws-", 0).factory());
        taskScheduler.initialize();
        stompClient.setTaskScheduler(taskScheduler);

        return stompClient;
    }

    @Bean
    public ScheduledExecutorService webSocketScheduledExecutor() {
        return Executors.newScheduledThreadPool(2);
    }
}