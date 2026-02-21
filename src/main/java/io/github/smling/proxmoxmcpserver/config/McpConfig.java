package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class McpConfig {
    private String host = "127.0.0.1";
    private int port = 8000;
    private McpTransport transport = McpTransport.STDIO;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public McpTransport getTransport() {
        return transport;
    }

    @JsonProperty("transport")
    public void setTransport(String transport) {
        this.transport = McpTransport.fromString(transport);
    }
}
