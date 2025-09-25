package id.segari.printer.segariprintermiddleware.common.dto.update;

public record VersionInfo(
        String currentVersion,
        String latestVersion,
        boolean updateAvailable,
        String downloadUrl,
        String releaseNotes
) {
}