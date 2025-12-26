package ru.mtuci.coursemanagement.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.client.RestTemplate;

import java.net.*;
import java.util.Set;

@RestController
public class ProxyController {

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "example.com",
            "httpbin.org"
    );

    @GetMapping("/api/proxy")
    public String proxy(@RequestParam("targetUrl") String targetUrl, HttpServletRequest req) throws Exception {

        HttpSession s = req.getSession(false);
        if (s == null || !"TEACHER".equals(String.valueOf(s.getAttribute("role")))) {
            throw new SecurityException("Forbidden");
        }

        URI uri = new URI(targetUrl);

        // A01/A05: разрешаем только http/https
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http/https allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host is required");
        }

        // A01: allowlist доменов (иначе любой внешний ресурс = open proxy)
        if (!ALLOWED_HOSTS.contains(host.toLowerCase())) {
            throw new SecurityException("Host is not allowed");
        }

        // A01: запрет на localhost/приватные/линк-локал адреса (SSRF)
        InetAddress addr = InetAddress.getByName(host);
        if (isBlockedAddress(addr)) {
            throw new SecurityException("Address is not allowed");
        }

        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(5000);

        RestTemplate rt = new RestTemplate(f);
        return rt.getForObject(uri, String.class);
    }

    private static boolean isBlockedAddress(InetAddress a) {
        return a.isAnyLocalAddress()
                || a.isLoopbackAddress()
                || a.isLinkLocalAddress()
                || a.isSiteLocalAddress()
                || isPrivateIpv4(a);
    }

    private static boolean isPrivateIpv4(InetAddress a) {
        byte[] b = a.getAddress();
        if (b.length != 4) return false;
        int first = b[0] & 0xFF;
        int second = b[1] & 0xFF;

        // 10.0.0.0/8
        if (first == 10) return true;
        // 172.16.0.0/12
        if (first == 172 && (second >= 16 && second <= 31)) return true;
        // 192.168.0.0/16
        return first == 192 && second == 168;
    }
}
