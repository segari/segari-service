package id.segari.printer.segariprintermiddleware.service.impl.print_queue;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.exception.InternalBaseException;
import id.segari.printer.segariprintermiddleware.service.PrintQueueService;
import id.segari.printer.segariprintermiddleware.service.PrinterService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    private static final int MAX_QUEUES = 20;
    private static final int MAX_QUEUE_SIZE = 20;

    private final Map<Integer, LinkedBlockingQueue<PrinterPrintRequest>> printerQueues = new ConcurrentHashMap<>();
    private final Map<Integer, Thread> consumerThreads = new ConcurrentHashMap<>();
    private final PrinterService printerService;

    public PrintQueueServiceImpl(PrinterService printerService) {
        this.printerService = printerService;
    }

    @Override
    public void addToQueue(PrinterPrintRequest request) {
        final int printerId = request.id();
        if (printerQueues.size() >= MAX_QUEUES){
            throw new InternalBaseException(InternalResponseCode.PRINT_QUEUE_MAX_QUEUE, HttpStatus.BAD_REQUEST, "Cannot create queue for printer " + printerId + ". Maximum queues limit reached.");
        }
        final LinkedBlockingQueue<PrinterPrintRequest> queue = getQueue(printerId);
        final boolean added = queue.offer(request);
        if (!added) {
            throw new InternalBaseException(InternalResponseCode.PRINT_QUEUE_MAX_JOB, HttpStatus.BAD_REQUEST, "Queue for printer " + printerId + " is full. Maximum capacity: " + MAX_QUEUE_SIZE);
        }
    }

    private LinkedBlockingQueue<PrinterPrintRequest> getQueue(int printerId) {
        return printerQueues.computeIfAbsent(printerId, id -> {
            final LinkedBlockingQueue<PrinterPrintRequest> newQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
            startConsumerThread(id, newQueue);
            return newQueue;
        });
    }

    private void startConsumerThread(int printerId, LinkedBlockingQueue<PrinterPrintRequest> queue) {
        final Thread consumerThread = Thread.ofVirtual().start(new ConsumerThreadRun(queue, printerService));
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
        if (queue == null) return new ArrayList<>();
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
        final LinkedBlockingQueue<PrinterPrintRequest> queue = printerQueues.get(printerId);
        if (queue == null) return false;
        queue.clear();
        return true;
    }

    @Override
    public void clearAllQueues() {
        for (Map.Entry<Integer, LinkedBlockingQueue<PrinterPrintRequest>> entry : printerQueues.entrySet()) {
            entry.getValue().clear();
        }
    }

    @Override
    public void removePrinterQueue(int printerId) {
        final Thread consumerThread = consumerThreads.remove(printerId);
        if (consumerThread != null) consumerThread.interrupt();
        printerQueues.remove(printerId);
    }

    @PreDestroy
    public void shutdown() {
        for (Thread thread : consumerThreads.values()) thread.interrupt();
        consumerThreads.clear();
        printerQueues.clear();
    }
}