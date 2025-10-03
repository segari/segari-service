package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.PrinterUsb;
import id.segari.service.common.dto.printer.connect.PrinterConnectRequest;
import id.segari.service.common.dto.printer.connect.PrinterConnectResponse;
import id.segari.service.common.dto.printer.disconnect.PrinterDisconnectResponse;
import id.segari.service.common.dto.printer.print.PrinterPrintRequest;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.PrintQueueService;
import id.segari.service.service.PrinterService;
import id.segari.service.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/printer")
@Validated
public class PrinterController {
    private final PrinterService printerService;
    private final PrintQueueService printQueueService;
    private final UrlService urlService;

    public PrinterController(PrinterService printerService, PrintQueueService printQueueService, UrlService urlService) {
        this.printerService = printerService;
        this.printQueueService = printQueueService;
        this.urlService = urlService;
    }

    @GetMapping("/connected/{id}")
    public SuccessResponse<Boolean> isConnected(@PathVariable int id){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, printerService.isConnected(id));
    }

    @GetMapping("/ping")
    public SuccessResponse<String> ping(){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, "PONG");
    }

    @PostMapping("/connect")
    public SuccessResponse<PrinterConnectResponse> connect(@Valid @RequestBody PrinterConnectRequest request){
        final PrinterConnectResponse response = printerService.connect(request);
        return new SuccessResponse<>(response.code(), response);
    }

    @PostMapping("/print")
    public SuccessResponse<String> print(@Valid @RequestBody PrinterPrintRequest request) {
        printQueueService.addToQueue(request);
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, "Print job added to queue");
    }

    @DeleteMapping("/disconnect/{id}")
    public SuccessResponse<PrinterDisconnectResponse> disconnect(@PathVariable int id){
        final PrinterDisconnectResponse response = printerService.disconnect(id);
        printQueueService.removePrinterQueue(id);
        return new SuccessResponse<>(response.code(), response);
    }

    @GetMapping("/usb")
    public SuccessResponse<List<PrinterUsb>> getUsb(){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, printerService.getUsb());
    }

    @GetMapping("/print-domain")
    public SuccessResponse<String> getPrintDomain(){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, urlService.getPrintDomain());
    }
}
