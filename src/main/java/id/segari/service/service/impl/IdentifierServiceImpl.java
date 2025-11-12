package id.segari.service.service.impl;

import id.segari.service.service.IdentifierService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class IdentifierServiceImpl implements IdentifierService {

    @Override
    public String get() {
        try {
            if (!isWindows()) return null;
            final Process process = getProcess();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("MachineGuid")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            return parts[parts.length - 1];
                        }
                    }
                }
            }
            process.waitFor();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    private Process getProcess() throws IOException {
        final ProcessBuilder processBuilder = new ProcessBuilder(
            "reg", "query",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography",
            "/v", "MachineGuid"
        );
        return processBuilder.start();
    }
}
