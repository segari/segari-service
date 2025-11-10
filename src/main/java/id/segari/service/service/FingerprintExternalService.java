package id.segari.service.service;

public interface FingerprintExternalService {
    void sync(long warehouseId);
    void add(String employeeId);
}
