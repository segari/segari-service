package id.segari.printer.segariprintermiddleware.service.impl;

import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import id.segari.printer.segariprintermiddleware.service.PrinterService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PrintQueueServiceImpl implements PrintQueueService {
    private static final Logger logger = LoggerFactory.getLogger(PrintQueueServiceImpl.class);
    private static final int MAX_QUEUES = 20;
    private static final int MAX_QUEUE_SIZE = 20;

    private final Map<Integer, LinkedBlockingQueue<PrinterPrintRequest>> printerQueues = new ConcurrentHashMap<>();
    private final Map<Integer, Thread> consumerThreads = new ConcurrentHashMap<>();
    private final AtomicInteger queueCount = new AtomicInteger(0);
    private final PrinterService printerService;
    private ExecutorService executorService;

    public PrintQueueServiceImpl(PrinterService printerService) {
        this.printerService = printerService;
    }

    @PostConstruct
    public void initialize() {
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        logger.info("PrintQueueService initialized with virtual threads");
    }

    @Override
    public void addToQueue(PrinterPrintRequest request) {
        int printerId = request.id();

        LinkedBlockingQueue<PrinterPrintRequest> queue = printerQueues.computeIfAbsent(printerId, id -> {
            if (queueCount.get() >= MAX_QUEUES) {
                logger.warn("Maximum number of queues ({}) reached. Cannot create queue for printer {}", MAX_QUEUES, id);
                return null;
            }

            LinkedBlockingQueue<PrinterPrintRequest> newQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
            queueCount.incrementAndGet();
            startConsumerThread(id, newQueue);
            logger.info("Created new queue for printer {}", id);
            return newQueue;
        });

        if (queue == null) {
            throw new RuntimeException("Cannot create queue for printer " + printerId + ". Maximum queues limit reached.");
        }

        boolean added = queue.offer(request);
        if (!added) {
            throw new RuntimeException("Queue for printer " + printerId + " is full. Maximum capacity: " + MAX_QUEUE_SIZE);
        }

        logger.debug("Added print job to queue for printer {}. Queue size: {}", printerId, queue.size());
    }

    private void startConsumerThread(int printerId, LinkedBlockingQueue<PrinterPrintRequest> queue) {
        Thread consumerThread = Thread.ofVirtual().start(() -> {
            logger.info("Started consumer thread for printer {}", printerId);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PrinterPrintRequest request = queue.take();
                    logger.debug("Processing print job for printer {}", printerId);

                    printerService.print(request);
                    logger.debug("Successfully processed print job for printer {}", printerId);

                } catch (InterruptedException e) {
                    logger.info("Consumer thread for printer {} was interrupted", printerId);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error processing print job for printer {}: {}", printerId, e.getMessage(), e);
                }
            }

            logger.info("Consumer thread for printer {} stopped", printerId);
        });

        consumerThreads.put(printerId, consumerThread);
    }

    @Override
    public int getQueueSize(int printerId) {
        LinkedBlockingQueue<PrinterPrintRequest> queue = printerQueues.get(printerId);
        return queue != null ? queue.size() : 0;
    }

    @Override
    public int getTotalQueues() {
        return printerQueues.size();
    }

    @Override
    public List<PrinterPrintRequest> getPendingJobs(int printerId) {
        LinkedBlockingQueue<PrinterPrintRequest> queue = printerQueues.get(printerId);
        if (queue == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(queue);
    }

    @Override
    public Map<Integer, Integer> getAllQueueSizes() {
        return printerQueues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }

    @Override
    public boolean clearQueue(int printerId) {
        LinkedBlockingQueue<PrinterPrintRequest> queue = printerQueues.get(printerId);
        if (queue == null) {
            return false;
        }

        int clearedJobs = queue.size();
        queue.clear();
        logger.info("Cleared {} jobs from queue for printer {}", clearedJobs, printerId);
        return true;
    }

    @Override
    public void clearAllQueues() {
        int totalCleared = 0;
        for (Map.Entry<Integer, LinkedBlockingQueue<PrinterPrintRequest>> entry : printerQueues.entrySet()) {
            int queueSize = entry.getValue().size();
            entry.getValue().clear();
            totalCleared += queueSize;
        }
        logger.info("Cleared {} total jobs from all queues", totalCleared);
    }

    @Override
    public void removePrinterQueue(int printerId) {
        Thread consumerThread = consumerThreads.remove(printerId);
        if (consumerThread != null) {
            consumerThread.interrupt();
        }

        LinkedBlockingQueue<PrinterPrintRequest> removedQueue = printerQueues.remove(printerId);
        if (removedQueue != null) {
            queueCount.decrementAndGet();
            logger.info("Removed queue for printer {} with {} pending jobs", printerId, removedQueue.size());
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down PrintQueueService...");

        for (Thread thread : consumerThreads.values()) {
            thread.interrupt();
        }

        consumerThreads.clear();
        printerQueues.clear();

        if (executorService != null) {
            executorService.shutdown();
        }

        logger.info("PrintQueueService shutdown complete");
    }
}