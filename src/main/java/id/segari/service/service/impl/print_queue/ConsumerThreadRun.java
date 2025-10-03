package id.segari.service.service.impl.print_queue;

import id.segari.service.common.dto.printer.print.PrinterPrintRequest;
import id.segari.service.service.PrinterService;

import java.util.concurrent.LinkedBlockingQueue;

public class ConsumerThreadRun implements Runnable{
    private final LinkedBlockingQueue<PrinterPrintRequest> queue;
    private final PrinterService printerService;

    public ConsumerThreadRun(LinkedBlockingQueue<PrinterPrintRequest> queue, PrinterService printerService) {
        this.queue = queue;
        this.printerService = printerService;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final PrinterPrintRequest request = queue.take();
                printerService.print(request);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception _) {}
        }
    }
}
