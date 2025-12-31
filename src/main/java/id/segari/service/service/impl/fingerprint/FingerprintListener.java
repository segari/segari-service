package id.segari.service.service.impl.fingerprint;

import com.zkteco.biometric.FingerprintSensorEx;

import java.util.concurrent.atomic.AtomicLong;

public abstract class FingerprintListener implements Runnable {
    private final byte[] template = new byte[2048];
    private final int[] templateLen = new int[1];
    private final byte[] imgBuf;
    private final AtomicLong device;

    protected FingerprintListener(AtomicLong device) {
        final byte[] paramValue = new byte[4];
        final int[] size = new int[1];
        size[0] = 4;
        FingerprintSensorEx.GetParameters(device.get(), 1, paramValue, size);
        final int width = byteArrayToInt(paramValue);
        size[0] = 4;
        FingerprintSensorEx.GetParameters(device.get(), 2, paramValue, size);
        final int height = byteArrayToInt(paramValue);
        this.imgBuf = new byte[width*height];
        this.device = device;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            templateLen[0] = 2048;
            int acquired = FingerprintSensorEx.AcquireFingerprint(device.get(), imgBuf, template, templateLen);
            if (acquired != 0) {
                // -8 means no finger detected, which is normal
                // Add a small delay to avoid busy-waiting
                try {
                    Thread.sleep(100); // Wait 100ms before trying again
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            byte[] paramValue = new byte[4];
            int[] size = new int[1];
            size[0] = 4;
            int nFakeStatus = 0;
            //GetFakeStatus
            int fakeStatus = FingerprintSensorEx.GetParameters(device.get(), 2004, paramValue, size);
            nFakeStatus = byteArrayToInt(paramValue);
            if (0 == fakeStatus && (byte)(nFakeStatus & 31) != 31)
            {
                continue;
            }
            onAcquireFingerprint(template);
        }
    }

    public static int byteArrayToInt(byte[] bytes) {
        int number = bytes[0] & 0xFF;
        number |= ((bytes[1] << 8) & 0xFF00);
        number |= ((bytes[2] << 16) & 0xFF0000);
        number |= ((bytes[3] << 24) & 0xFF000000);
        return number;
    }

    public abstract void onAcquireFingerprint(byte[] template);
}
