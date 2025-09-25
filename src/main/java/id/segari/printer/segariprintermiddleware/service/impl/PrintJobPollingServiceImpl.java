package id.segari.printer.segariprintermiddleware.service.impl;

import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrintJobResponse;
import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.service.PrintJobPollingService;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PrintJobPollingServiceImpl implements PrintJobPollingService {
    private static final Logger logger = LoggerFactory.getLogger(PrintJobPollingServiceImpl.class);

    private final RestTemplate restTemplate;
    private final PrintQueueService printQueueService;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private final AtomicInteger polledJobsCount = new AtomicInteger(0);

    @Value("${polling.backend.url}")
    private String backendUrl;

    @Value("${polling.interval.ms:500}")
    private long pollingIntervalMs;

    private ScheduledFuture<?> pollingTask;

    public PrintJobPollingServiceImpl(RestTemplate restTemplate,
                                     PrintQueueService printQueueService,
                                     ScheduledExecutorService scheduledExecutor) {
        this.restTemplate = restTemplate;
        this.printQueueService = printQueueService;
        this.scheduledExecutor = scheduledExecutor;
    }

    @Override
    public synchronized void startPolling() {
        if (isPolling.get()) {
            logger.warn("Polling is already active");
            return;
        }

        logger.info("Starting print job polling with interval {}ms", pollingIntervalMs);

        pollingTask = scheduledExecutor.scheduleWithFixedDelay(
            this::pollPrintJobs,
            0,
            pollingIntervalMs,
            TimeUnit.MILLISECONDS
        );

        isPolling.set(true);
        logger.info("Print job polling started successfully");
    }

    @Override
    public synchronized void stopPolling() {
        if (!isPolling.get()) {
            logger.warn("Polling is not currently active");
            return;
        }

        logger.info("Stopping print job polling...");

        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }

        isPolling.set(false);
        logger.info("Print job polling stopped");
    }

    @Override
    public boolean isPolling() {
        return isPolling.get();
    }

    @Override
    public int getPolledJobsCount() {
        return polledJobsCount.get();
    }

    private void pollPrintJobs() {
        try {
            logger.debug("Polling for print jobs from: {}", backendUrl);

            PrintJobResponse response = restTemplate.getForObject(backendUrl, PrintJobResponse.class);

            if (response != null && response.printJobs() != null && !response.printJobs().isEmpty()) {
                logger.debug("Received {} print jobs from backend", response.printJobs().size());

                for (PrinterPrintRequest printJob : response.printJobs()) {
                    try {
                        printQueueService.addToQueue(printJob);
                        polledJobsCount.incrementAndGet();
                        logger.debug("Added print job to queue for printer {}", printJob.id());
                    } catch (Exception e) {
                        logger.error("Failed to add print job to queue for printer {}: {}",
                                   printJob.id(), e.getMessage());
                    }
                }
            } else {
                logger.debug("No print jobs received from backend");
            }

        } catch (Exception e) {
            logger.error("Error polling print jobs from backend: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down PrintJobPollingService...");
        stopPolling();
        logger.info("PrintJobPollingService shutdown complete");
    }
}