package io.github.smling.proxmoxmcpserver.core;

import io.github.smling.proxmoxmcpserver.config.AuthConfig;
import io.github.smling.proxmoxmcpserver.config.ProxmoxConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates and validates a Proxmox API client for tool usage.
 */
public class ProxmoxManager {
    private static final Logger logger = LoggerFactory.getLogger(ProxmoxManager.class);
    private final ProxmoxClient apiClient;

    /**
     * Builds a manager from configuration settings.
     *
     * @param proxmoxConfig the Proxmox connection config
     * @param authConfig the authentication config
     */
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

    /**
     * Builds a manager from an existing Proxmox client (primarily for tests).
     *
     * @param client the Proxmox client
     * @param validateConnection whether to validate the connection
     */
    ProxmoxManager(ProxmoxClient client, boolean validateConnection) {
        this.apiClient = validateConnection ? testConnection(client) : client;
    }

    /**
     * Tests the API connection and returns the usable client.
     *
     * @param client the configured client
     * @return the validated client
     */
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

    /**
     * Returns the validated Proxmox API client.
     *
     * @return the API client
     */
    public ProxmoxClient getApi() {
        return apiClient;
    }
}
