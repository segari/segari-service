package id.segari.printer.segariprintermiddleware.controller;

import id.segari.printer.segariprintermiddleware.common.InternalResponseCode;
import id.segari.printer.segariprintermiddleware.common.dto.version.VersionResponse;
import id.segari.printer.segariprintermiddleware.common.response.SuccessResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/versions")
public class VersionController {

    @GetMapping
    public SuccessResponse<VersionResponse> get(){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, new VersionResponse(
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("java.runtime.version"),
                System.getProperty("java.vm.name"),
                System.getProperty("java.vm.version"),
                System.getProperty("java.specification.version")
        ));
    }
}
