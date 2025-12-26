package ru.mtuci.coursemanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class PluginLoader {

    /**
     * For demo/security: allow only local file plugins inside a dedicated directory.
     * Example: app.plugin.url=file:plugins/plugin.jar
     */
    @Value("${app.plugin.url:}")
    private String pluginUrl;

    @Value("${app.plugin.allowed-dir:plugins}")
    private String allowedDir;

    public void tryLoad() {
        if (pluginUrl == null || pluginUrl.isBlank()) return;

        try {
            URI uri = URI.create(pluginUrl);

            // Block remote loading (RCE risk). Allow only file: URLs.
            if (!"file".equalsIgnoreCase(uri.getScheme())) {
                log.warn("Plugin loading blocked (only file: URLs allowed): {}", pluginUrl);
                return;
            }

            Path base = Paths.get(allowedDir).toAbsolutePath().normalize();
            Path pluginPath = Paths.get(uri).toAbsolutePath().normalize();

            // Allow only files under allowedDir
            if (!pluginPath.startsWith(base)) {
                log.warn("Plugin loading blocked (outside allowed dir {}): {}", base, pluginPath);
                return;
            }

            URL url = pluginPath.toUri().toURL();
            try (URLClassLoader cl = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader())) {
                Class<?> clazz = Class.forName("com.example.PluginMain", true, cl);
                Method m = clazz.getDeclaredMethod("init");
                m.invoke(null);
                log.info("Plugin loaded from: {}", pluginPath);
            }
        } catch (Exception e) {
            log.error("Plugin load error: ", e);
        }
    }
}
