package id.segari.service.controller;

import id.segari.service.common.InternalResponseCode;
import id.segari.service.common.dto.identifier.IdentifierResponse;
import id.segari.service.common.response.SuccessResponse;
import id.segari.service.service.IdentifierService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/identifier")
public class IdentifierController {
    private final IdentifierService identifierService;

    public IdentifierController(IdentifierService identifierService) {
        this.identifierService = identifierService;
    }

    @GetMapping
    public SuccessResponse<IdentifierResponse> get(){
        return new SuccessResponse<>(InternalResponseCode.SUCCESS, new IdentifierResponse(identifierService.get()));
    }
}
