package id.segari.service.service;

public interface FingerprintService {
    void sync(long shippingPointId);
    void add(long internalToolsUserId);
    void enroll();
}
