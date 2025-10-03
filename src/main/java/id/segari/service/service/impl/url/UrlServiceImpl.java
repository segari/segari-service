package id.segari.service.service.impl.url;

import id.segari.service.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Service
public class UrlServiceImpl implements UrlService {

    @Value("${server.port}")
    private String port;

    @Override
    public String getPrintDomain() {
        try {
            String localIp = getLocalIpAddress();
            return String.format("http://%s:%s", localIp, port);
        } catch (Exception e) {
            return "http://localhost:54124";
        }
    }

    private String getLocalIpAddress() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isLoopbackAddress() && address.isSiteLocalAddress()
                    && !address.getHostAddress().contains(":")) {
                    return address.getHostAddress();
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }
}
