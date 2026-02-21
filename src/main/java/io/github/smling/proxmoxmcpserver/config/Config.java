package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Root configuration for the Proxmox MCP server.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    private ProxmoxConfig proxmox;
    private AuthConfig auth;
    private McpConfig mcp = new McpConfig();

    /**
     * Returns the Proxmox connection configuration.
     *
     * @return the Proxmox configuration
     */
    public ProxmoxConfig getProxmox() {
        return proxmox;
    }

    /**
     * Sets the Proxmox connection configuration.
     *
     * @param proxmox the Proxmox configuration
     */
    public void setProxmox(ProxmoxConfig proxmox) {
        this.proxmox = proxmox;
    }

    /**
     * Returns the authentication configuration.
     *
     * @return the authentication configuration
     */
    public AuthConfig getAuth() {
        return auth;
    }

    /**
     * Sets the authentication configuration.
     *
     * @param auth the authentication configuration
     */
    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    /**
     * Returns the MCP transport configuration.
     *
     * @return the MCP configuration
     */
    public McpConfig getMcp() {
        return mcp;
    }

    /**
     * Sets the MCP transport configuration.
     *
     * @param mcp the MCP configuration
     */
    public void setMcp(McpConfig mcp) {
        this.mcp = mcp;
    }
}
