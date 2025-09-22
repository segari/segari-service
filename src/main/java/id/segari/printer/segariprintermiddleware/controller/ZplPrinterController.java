package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.service.ZplPrinterService;
import id.segari.printer.segariprintermiddleware.zpl.ZplCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/zpl")
@RequiredArgsConstructor
public class ZplPrinterController {

    private final ZplPrinterService zplPrinterService;


    /**
     * Print text to USB printer
     */
    @PostMapping("/usb/print/text")
    public Map<String, Object> printTextToUSB(@RequestParam int vendorId,
                                             @RequestParam int productId,
                                             @RequestParam String text,
                                             @RequestParam(defaultValue = "50") int x,
                                             @RequestParam(defaultValue = "50") int y,
                                             @RequestParam(defaultValue = "30") int fontSize) {
        Map<String, Object> response = new HashMap<>();

        String zplCommand = ZplCommand.createTextLabel(text, x, y, fontSize);
        boolean success = zplPrinterService.printToUSB(vendorId, productId, zplCommand);

        response.put("success", success);
        response.put("message", success ? "Text printed successfully" : "Failed to print text");
        response.put("zplCommand", zplCommand);
        return response;
    }

    /**
     * Print QR code to USB printer
     */
    @PostMapping("/usb/print/qr")
    public Map<String, Object> printQRToUSB(@RequestParam int vendorId,
                                           @RequestParam int productId,
                                           @RequestParam String data,
                                           @RequestParam(defaultValue = "50") int x,
                                           @RequestParam(defaultValue = "50") int y,
                                           @RequestParam(defaultValue = "5") int size) {
        Map<String, Object> response = new HashMap<>();

        String zplCommand = ZplCommand.createQRLabel(data, x, y, size);
        boolean success = zplPrinterService.printToUSB(vendorId, productId, zplCommand);

        response.put("success", success);
        response.put("message", success ? "QR code printed successfully" : "Failed to print QR code");
        response.put("zplCommand", zplCommand);
        return response;
    }

    /**
     * Print custom ZPL command to USB printer
     */
    @PostMapping("/usb/print/raw")
    public Map<String, Object> printRawZplToUSB(@RequestParam int vendorId,
                                               @RequestParam int productId,
                                               @RequestBody String zplCommand) {
        Map<String, Object> response = new HashMap<>();

        boolean success = zplPrinterService.printToUSB(vendorId, productId, zplCommand);

        response.put("success", success);
        response.put("message", success ? "ZPL command sent successfully" : "Failed to send ZPL command");
        response.put("zplCommand", zplCommand);
        return response;
    }

    /**
     * Create advanced ZPL label
     */
    @PostMapping("/create/label")
    public Map<String, Object> createAdvancedLabel(@RequestBody Map<String, Object> labelRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            ZplCommand zpl = new ZplCommand();

            // Set label properties if provided
            if (labelRequest.containsKey("width")) {
                zpl.setPrintWidth((Integer) labelRequest.get("width"));
            }
            if (labelRequest.containsKey("length")) {
                zpl.setLabelLength((Integer) labelRequest.get("length"));
            }
            if (labelRequest.containsKey("quantity")) {
                zpl.setPrintQuantity((Integer) labelRequest.get("quantity"));
            }

            // Add elements from request
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements = (List<Map<String, Object>>) labelRequest.get("elements");

            if (elements != null) {
                for (Map<String, Object> element : elements) {
                    String type = (String) element.get("type");
                    int x = (Integer) element.get("x");
                    int y = (Integer) element.get("y");

                    switch (type.toLowerCase()) {
                        case "text":
                            String text = (String) element.get("text");
                            int fontSize = (Integer) element.getOrDefault("fontSize", 30);
                            zpl.addText(x, y, text, fontSize);
                            break;

                        case "qr":
                            String qrData = (String) element.get("data");
                            int qrSize = (Integer) element.getOrDefault("size", 5);
                            zpl.addQRCode(x, y, qrData, qrSize);
                            break;

                        case "barcode":
                            String barcodeData = (String) element.get("data");
                            int height = (Integer) element.getOrDefault("height", 100);
                            zpl.addBarcode128(x, y, barcodeData, height, 2);
                            break;

                        case "line":
                            int width = (Integer) element.getOrDefault("width", 100);
                            int thickness = (Integer) element.getOrDefault("thickness", 3);
                            zpl.addLine(x, y, width, thickness);
                            break;

                        case "box":
                            int boxWidth = (Integer) element.getOrDefault("width", 100);
                            int boxHeight = (Integer) element.getOrDefault("height", 100);
                            int boxThickness = (Integer) element.getOrDefault("thickness", 3);
                            zpl.addBox(x, y, boxWidth, boxHeight, boxThickness);
                            break;
                    }
                }
            }

            String zplCommand = zpl.build();

            response.put("success", true);
            response.put("zplCommand", zplCommand);
            response.put("message", "ZPL command created successfully");

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create ZPL command: " + e.getMessage());
        }

        return response;
    }

    /**
     * List available USB printers
     */
    @GetMapping("/usb/list")
    public Map<String, Object> listUSBPrinters() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> printers = zplPrinterService.listUSBPrinters();
            response.put("success", true);
            response.put("printers", printers);
            response.put("count", printers.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to list USB printers: " + e.getMessage());
        }

        return response;
    }


    /**
     * Close USB connection
     */
    @DeleteMapping("/usb/close")
    public Map<String, Object> closeUSBConnection(@RequestParam int vendorId,
                                                 @RequestParam int productId) {
        Map<String, Object> response = new HashMap<>();

        zplPrinterService.closeUSBConnection(vendorId, productId);

        response.put("success", true);
        response.put("message", "USB connection closed");
        return response;
    }
}