package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Connection settings for the Proxmox API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxmoxConfig {
    private String host;
    private int port = 8006;
    @JsonProperty("verify_ssl")
    private boolean verifySsl = true;
    private String service = "PVE";

    /**
     * Returns the Proxmox host address.
     *
     * @return the host address
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the Proxmox host address.
     *
     * @param host the host address
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the Proxmox API port.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the Proxmox API port.
     *
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns whether TLS certificates are verified.
     *
     * @return {@code true} when certificate validation is enabled
     */
    public boolean isVerifySsl() {
        return verifySsl;
    }

    /**
     * Sets whether TLS certificates are verified.
     *
     * @param verifySsl whether to validate certificates
     */
    public void setVerifySsl(boolean verifySsl) {
        this.verifySsl = verifySsl;
    }

    /**
     * Returns the Proxmox realm/service name.
     *
     * @return the service name
     */
    public String getService() {
        return service;
    }

    /**
     * Sets the Proxmox realm/service name.
     *
     * @param service the service name
     */
    public void setService(String service) {
        this.service = service;
    }
}
