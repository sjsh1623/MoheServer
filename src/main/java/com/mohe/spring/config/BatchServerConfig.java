package com.mohe.spring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "batch")
@Getter
@Setter
public class BatchServerConfig {

    private Service service = new Service();
    private List<RemoteServer> remoteServers = new ArrayList<>();

    @Getter
    @Setter
    public static class Service {
        private String url = "http://localhost:8081";
    }

    @Getter
    @Setter
    public static class RemoteServer {
        private String name;
        private String url;
        private boolean enabled = false;
        private String dockerHost;  // Tailscale IP or hostname
        private int dockerPort = 2375;  // Docker TCP API port
    }

    public List<RemoteServer> getEnabledServers() {
        return remoteServers.stream()
                .filter(RemoteServer::isEnabled)
                .toList();
    }
}
