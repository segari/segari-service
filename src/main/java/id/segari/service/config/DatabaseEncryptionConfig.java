package id.segari.service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

@Configuration
@ConditionalOnProperty(name = "segari.database.encryption.enabled", havingValue = "true")
public class DatabaseEncryptionConfig {

    /**
     * Generates a machine-specific encryption key based on hardware characteristics.
     * This key is derived from the machine's MAC address and system properties.
     * The key is never stored - regenerated on each startup.
     */
    private String generateMachineSpecificKey() {
        try {
            StringBuilder machineId = new StringBuilder();

            // Get MAC address from first available network interface
            Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
            while (networks.hasMoreElements()) {
                NetworkInterface network = networks.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    for (byte b : mac) {
                        machineId.append(String.format("%02X", b));
                    }
                    break;
                }
            }

            // Add Windows machine name and username for additional uniqueness
            machineId.append(System.getProperty("user.name"));
            machineId.append(System.getenv("COMPUTERNAME"));

            // Hash the machine ID to create a fixed-length key
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(machineId.toString().getBytes(StandardCharsets.UTF_8));

            // H2 expects the encryption key in specific format
            // Take first 16 bytes and encode as hex string
            StringBuilder hexKey = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                hexKey.append(String.format("%02x", hash[i]));
            }

            return hexKey.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate machine-specific encryption key", e);
        }
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String baseUrl = System.getProperty("spring.datasource.url",
                "jdbc:h2:file:" + System.getProperty("APP_DATA_DIR", "./data") + "/segari");

        // Generate machine-specific encryption key
        String encryptionKey = generateMachineSpecificKey();

        // Add H2 encryption parameters to the URL
        String encryptedUrl = baseUrl + ";CIPHER=AES";

        return DataSourceBuilder.create()
                .url(encryptedUrl)
                .username("sa")
                .password(encryptionKey + " " + encryptionKey) // H2 format: "filePassword userPassword"
                .driverClassName("org.h2.Driver")
                .build();
    }
}