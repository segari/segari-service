package id.segari.printer.segariprintermiddleware.service;

public interface ZebraPrinterService {
    void connect(String address);
    void disconnect(String address);
    void print(String address, String text);
    String getUsb();
}
