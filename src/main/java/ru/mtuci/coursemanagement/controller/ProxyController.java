package ru.mtuci.coursemanagement.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;

@RestController
public class ProxyController {

    /**
     * Comma-separated allowlist of hostnames that can be proxied.
     * Example: app.proxy.allowed-hosts=api.example.com,example.org
     */
    @Value("${app.proxy.allowed-hosts:}")
    private String allowedHosts;

    @GetMapping("/api/proxy")
    public ResponseEntity<String> proxy(@RequestParam("targetUrl") String targetUrl) {
        URI uri;
        try {
            uri = new URI(targetUrl);
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid URL");
        }

        // Allow only http/https
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only http/https allowed");
        }

        // Disallow userinfo (user:pass@host)
        if (uri.getUserInfo() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("UserInfo is not allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Host is required");
        }

        // Check allowlist
        boolean allowlistEnabled = allowedHosts != null && !allowedHosts.isBlank();
        List<String> allow = List.of(allowedHosts.split("\\s*,\\s*"));
        if (!allowlistEnabled || allow.stream().noneMatch(h -> h.equalsIgnoreCase(host))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Target host is not allowed");
        }

        // Prevent SSRF to private / local networks (including DNS -> private IP)
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress a : addrs) {
                if (isPrivateOrLocal(a)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Private/local addresses are not allowed");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to resolve host");
        }

        // Safe timeouts
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(10).toMillis());

        RestTemplate rt = new RestTemplate(rf);
        String body = rt.getForObject(uri, String.class);
        return ResponseEntity.ok(body);
    }

    private boolean isPrivateOrLocal(InetAddress addr) {
        return addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress();
    }
}
