package id.segari.printer.segariprintermiddleware.service.impl.url;

import id.segari.printer.segariprintermiddleware.service.UrlService;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Service
public class UrlServiceImpl implements UrlService {

    @Override
    public String getPrintDomain() {
        try {
            String localIp = getLocalIpAddress();
            return String.format("http://%s:54124/v1/printer/print", localIp);
        } catch (Exception e) {
            return "http://localhost:54124/v1/printer/print";
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
