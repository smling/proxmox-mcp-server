package io.github.smling.proxmoxmcpserver.core;

import io.github.smling.proxmoxmcpserver.config.AuthConfig;
import io.github.smling.proxmoxmcpserver.config.ProxmoxConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxmoxManager {
    private static final Logger logger = LoggerFactory.getLogger(ProxmoxManager.class);
    private final ProxmoxClient apiClient;

    public ProxmoxManager(ProxmoxConfig proxmoxConfig, AuthConfig authConfig) {
        ProxmoxClient client = new ProxmoxClient(
            proxmoxConfig.getHost(),
            proxmoxConfig.getPort(),
            proxmoxConfig.isVerifySsl(),
            authConfig.getUser(),
            authConfig.getTokenName(),
            authConfig.getTokenValue()
        );
        this.apiClient = testConnection(client);
    }

    private ProxmoxClient testConnection(ProxmoxClient client) {
        try {
            logger.info("Connecting to Proxmox host: {}", client.getBaseUri().getHost());
            client.testConnection();
            logger.info("Successfully connected to Proxmox API");
            return client;
        } catch (Exception e) {
            logger.error("Failed to connect to Proxmox", e);
            throw new RuntimeException("Failed to connect to Proxmox: " + e.getMessage(), e);
        }
    }

    public ProxmoxClient getApi() {
        return apiClient;
    }
}
