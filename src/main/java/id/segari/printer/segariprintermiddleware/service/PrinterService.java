package id.segari.printer.segariprintermiddleware.service;

import id.segari.printer.segariprintermiddleware.common.dto.PrinterUsb;
import id.segari.printer.segariprintermiddleware.common.dto.printer.PrinterConnectedResponse;
import id.segari.printer.segariprintermiddleware.common.dto.printer.connect.PrinterConnectRequest;
import id.segari.printer.segariprintermiddleware.common.dto.printer.connect.PrinterConnectResponse;
import id.segari.printer.segariprintermiddleware.common.dto.printer.disconnect.PrinterDisconnectResponse;
import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;

import java.util.List;

public interface PrinterService {
    PrinterConnectResponse connect(PrinterConnectRequest request);
    PrinterDisconnectResponse disconnect(int id);
    void print(PrinterPrintRequest request);
    List<PrinterUsb> getUsb();
    List<PrinterConnectedResponse> getConnected();
}
