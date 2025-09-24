package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.PrinterUsb;
import id.segari.printer.segariprintermiddleware.common.dto.printer.connect.PrinterConnectRequest;
import id.segari.printer.segariprintermiddleware.common.dto.printer.connect.PrinterConnectResponse;
import id.segari.printer.segariprintermiddleware.common.dto.printer.disconnect.PrinterDisconnectResponse;
import id.segari.printer.segariprintermiddleware.common.dto.printer.print.PrinterPrintRequest;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import id.segari.printer.segariprintermiddleware.service.PrinterService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/printer")
@Validated
public class PrinterController {
    private final PrinterService printerService;

    public PrinterController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @PostMapping("/connect")
    public SuccessResponse<PrinterConnectResponse> connect(@Valid @RequestBody PrinterConnectRequest request){
        final PrinterConnectResponse response = printerService.connect(request);
        return new SuccessResponse<>(response.code(), response);
    }

    @PostMapping("/print")
    public void print(@Valid @RequestBody PrinterPrintRequest request) {
        printerService.print(request);
    }

    @DeleteMapping("/disconnect/{id}")
    public SuccessResponse<PrinterDisconnectResponse> disconnect(@PathVariable int id){
        final PrinterDisconnectResponse response = printerService.disconnect(id);
        return new SuccessResponse<>(response.code(), response);
    }

    @GetMapping("/usb")
    public SuccessResponse<List<PrinterUsb>> getUsb(){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, printerService.getUsb());
    }
}
