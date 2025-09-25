package id.segari.printer.segariprintermiddleware.service;

import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;

import java.util.List;
import java.util.Map;

public interface PrintQueueService {
    void addToQueue(PrinterPrintRequest request);

    int getQueueSize(int printerId);

    int getTotalQueues();

    List<PrinterPrintRequest> getPendingJobs(int printerId);

    Map<Integer, Integer> getAllQueueSizes();

    boolean clearQueue(int printerId);

    void clearAllQueues();

    void removePrinterQueue(int printerId);
}