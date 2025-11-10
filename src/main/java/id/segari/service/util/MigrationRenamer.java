package id.segari.service.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Utility to rename SQL migration files without timestamp prefixes
 * to include timestamp prefixes.
 */
public class MigrationRenamer {

    // Pattern to detect if filename already has a timestamp (14 digits)
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^V\\d{14}__.*\\.sql$");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static void main(String[] args) {
        String migrationDir = "src/main/resources/db/migration";

        if (args.length > 0) {
            migrationDir = args[0];
        }

        renameMigrations(migrationDir);
    }

    public static void renameMigrations(String migrationDirPath) {
        File migrationDir = new File(migrationDirPath);

        if (!migrationDir.exists() || !migrationDir.isDirectory()) {
            System.err.println("Migration directory not found: " + migrationDirPath);
            return;
        }

        File[] files = migrationDir.listFiles((dir, name) -> name.endsWith(".sql"));

        if (files == null || files.length == 0) {
            System.out.println("No SQL files found in: " + migrationDirPath);
            return;
        }

        int renamedCount = 0;
        int skippedCount = 0;

        for (File file : files) {
            String filename = file.getName();

            // Skip if already has timestamp
            if (TIMESTAMP_PATTERN.matcher(filename).matches()) {
                System.out.println("Skipped (already has timestamp): " + filename);
                skippedCount++;
                continue;
            }

            // Generate new filename with timestamp prefix
            String timestamp = generateTimestamp(renamedCount);
            String newFilename;

            if (filename.startsWith("V")) {
                // If starts with V, replace everything before __ with timestamp
                int doubleUnderscoreIndex = filename.indexOf("__");
                if (doubleUnderscoreIndex > 0) {
                    newFilename = "V" + timestamp + filename.substring(doubleUnderscoreIndex);
                } else {
                    // No __, just add timestamp after V
                    newFilename = "V" + timestamp + "__" + filename.substring(1);
                }
            } else {
                // Doesn't start with V, add V + timestamp + __
                newFilename = "V" + timestamp + "__" + filename;
            }

            File newFile = new File(migrationDir, newFilename);

            try {
                Path source = file.toPath();
                Path target = newFile.toPath();
                Files.move(source, target);
                System.out.println("Renamed: " + filename + " -> " + newFilename);
                renamedCount++;
            } catch (IOException e) {
                System.err.println("Failed to rename " + filename + ": " + e.getMessage());
            }
        }

        System.out.println("\n=== Summary ===");
        System.out.println("Files renamed: " + renamedCount);
        System.out.println("Files skipped: " + skippedCount);
    }

    /**
     * Generate timestamp based on current time, adding seconds offset
     * to maintain ordering when renaming multiple files
     */
    private static String generateTimestamp(int offset) {
        LocalDateTime now = LocalDateTime.now().plusSeconds(offset);
        return now.format(TIMESTAMP_FORMAT);
    }
}