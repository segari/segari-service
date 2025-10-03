package id.segari.service.service.impl;

import id.segari.service.common.dto.identifier.IdentifierResponse;
import id.segari.service.service.IdentifierService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class IdentifierServiceImpl implements IdentifierService {

    @Override
    public IdentifierResponse get() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "reg", "query",
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography",
                "/v", "MachineGuid"
            );
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("MachineGuid")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            String guid = parts[parts.length - 1];
                            return new IdentifierResponse(guid);
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return new IdentifierResponse(null);
            }
        } catch (Exception e) {
            return new IdentifierResponse(null);
        }

        return new IdentifierResponse(null);
    }
}
