package id.segari.service.service.impl;

import id.segari.service.common.dto.fingerprint.FingerprintEnrollmentResponse;
import id.segari.service.common.dto.fingerprint.FingerprintIdentificationResponse;
import id.segari.service.common.dto.fingerprint.FingerprintStatusResponse;
import id.segari.service.db.enums.TemplateGroup;
import id.segari.service.service.FingerprintService;
import id.segari.service.service.impl.fingerprint.ZKTecoFingerprintServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@Primary
public class FingerprintServiceImpl implements FingerprintService {

    private final ReentrantLock lock = new ReentrantLock();
    private final ZKTecoFingerprintServiceImpl zkTecoImpl;

    public FingerprintServiceImpl(final ZKTecoFingerprintServiceImpl zkTecoImpl) {
        this.zkTecoImpl = zkTecoImpl;
    }

    @Override
    public void connect() {
        lock.lock();
        try {
            zkTecoImpl.connect();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void disconnect() {
        lock.lock();
        try {
            zkTecoImpl.disconnect();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FingerprintStatusResponse getFingerprintStatus() {
        lock.lock();
        try {
            return zkTecoImpl.getFingerprintStatus();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FingerprintEnrollmentResponse initEnrollment(final String employeeId, final TemplateGroup templateGroup) {
        lock.lock();
        try {
            return zkTecoImpl.initEnrollment(employeeId, templateGroup);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public FingerprintIdentificationResponse initIdentification() {
        lock.lock();
        try {
            return zkTecoImpl.initIdentification();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sync(final long warehouseId) {
        lock.lock();
        try {
            zkTecoImpl.sync(warehouseId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void sync(final long warehouseId, final long internalToolsUserId) {
        lock.lock();
        try {
            zkTecoImpl.sync(warehouseId, internalToolsUserId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void add(final String employeeId) {
        lock.lock();
        try {
            zkTecoImpl.add(employeeId);
        } finally {
            lock.unlock();
        }
    }
}
