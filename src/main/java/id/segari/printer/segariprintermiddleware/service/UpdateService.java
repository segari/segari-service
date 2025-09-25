package id.segari.printer.segariprintermiddleware.service;

import id.segari.printer.segariprintermiddleware.common.dto.update.VersionInfo;

public interface UpdateService {
    VersionInfo checkForUpdates();

    void downloadUpdate(String downloadUrl);

    boolean isUpdateDownloaded();

    void applyUpdate();

    String getCurrentVersion();
}