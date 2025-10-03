package id.segari.service.service;

import id.segari.service.common.dto.update.VersionInfo;

public interface UpdateService {
    VersionInfo checkForUpdates();

    void downloadUpdate(String downloadUrl);

    boolean isUpdateDownloaded();

    void applyUpdate();

    String getCurrentVersion();
}