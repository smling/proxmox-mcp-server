package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
    private ProxmoxConfig proxmox;
    private AuthConfig auth;
    private McpConfig mcp = new McpConfig();

    public ProxmoxConfig getProxmox() {
        return proxmox;
    }

    public void setProxmox(ProxmoxConfig proxmox) {
        this.proxmox = proxmox;
    }

    public AuthConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthConfig auth) {
        this.auth = auth;
    }

    public McpConfig getMcp() {
        return mcp;
    }

    public void setMcp(McpConfig mcp) {
        this.mcp = mcp;
    }
}
