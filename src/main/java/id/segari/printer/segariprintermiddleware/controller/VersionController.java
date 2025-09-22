package id.segari.printer.segariprintermiddleware.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/versions")
public class VersionController {

    @GetMapping
    public Map<String, String> get(){
        Map<String, String> versions = new HashMap<>();
        versions.put("java.version", System.getProperty("java.version"));
        versions.put("java.vendor", System.getProperty("java.vendor"));
        versions.put("java.runtime.version", System.getProperty("java.runtime.version"));
        versions.put("java.vm.name", System.getProperty("java.vm.name"));
        versions.put("java.vm.version", System.getProperty("java.vm.version"));
        versions.put("java.specification.version", System.getProperty("java.specification.version"));
        return versions;
    }
}
