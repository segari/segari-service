package id.segari.printer.segariprintermiddleware.service;

public interface PrintJobPollingService {
    void startPolling();

    void stopPolling();

    boolean isPolling();

    int getPolledJobsCount();
}