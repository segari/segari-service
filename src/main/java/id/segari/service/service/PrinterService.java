package id.segari.service.service;

import id.segari.service.common.dto.PrinterUsb;
import id.segari.service.common.dto.printer.PrinterConnectedResponse;
import id.segari.service.common.dto.printer.connect.PrinterConnectRequest;
import id.segari.service.common.dto.printer.connect.PrinterConnectResponse;
import id.segari.service.common.dto.printer.disconnect.PrinterDisconnectResponse;
import id.segari.service.common.dto.printer.print.PrinterPrintRequest;

import java.util.List;

public interface PrinterService {
    PrinterConnectResponse connect(PrinterConnectRequest request);
    PrinterDisconnectResponse disconnect(int id);
    void print(PrinterPrintRequest request);
    List<PrinterUsb> getAllPrinter();
    List<PrinterConnectedResponse> getConnected();
    boolean isConnected(int id);
}
