package id.segari.service.service.impl;

import id.segari.service.common.dto.fingerprint.FingerprintEnrollmentResponse;
import id.segari.service.common.dto.fingerprint.FingerprintIdentificationResponse;
import id.segari.service.common.dto.fingerprint.FingerprintStatusResponse;
import id.segari.service.db.enums.TemplateGroup;
import id.segari.service.exception.BaseException;
import id.segari.service.service.FingerprintService;
import id.segari.service.service.impl.fingerprint.ZKTecoFingerprintServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Primary
public class FingerprintServiceImpl implements FingerprintService {

    private static final long LOCK_TIMEOUT_SECONDS = 2;

    private final ReentrantLock lock = new ReentrantLock();
    private final ZKTecoFingerprintServiceImpl zkTecoImpl;

    public FingerprintServiceImpl(final ZKTecoFingerprintServiceImpl zkTecoImpl) {
        this.zkTecoImpl = zkTecoImpl;
    }

    private void acquireLock() {
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new BaseException("Failed to acquire fingerprint service lock within " + LOCK_TIMEOUT_SECONDS + " seconds");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("Lock acquisition interrupted: " + e.getMessage());
        }
    }

    @Override
    public void connect(long warehouseId, String sessionId) {
        acquireLock();
        try {
            zkTecoImpl.connect(warehouseId, sessionId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void disconnect() {
        acquireLock();
        try {
            zkTecoImpl.disconnect();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FingerprintStatusResponse getFingerprintStatus() {
        acquireLock();
        try {
            return zkTecoImpl.getFingerprintStatus();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FingerprintEnrollmentResponse initEnrollment(final String employeeId, final TemplateGroup templateGroup) {
        acquireLock();
        try {
            return zkTecoImpl.initEnrollment(employeeId, templateGroup);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FingerprintIdentificationResponse initIdentification() {
        acquireLock();
        try {
            return zkTecoImpl.initIdentification();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void initNone() {
        acquireLock();
        try {
            zkTecoImpl.initNone();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sync(final long warehouseId) {
        acquireLock();
        try {
            zkTecoImpl.sync(warehouseId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(final String employeeId, boolean adhoc) {
        acquireLock();
        try {
            zkTecoImpl.add(employeeId, adhoc);
        } finally {
            lock.unlock();
        }
    }
}
