package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP transport configuration for exposing tools.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpConfig {
    private String host = "127.0.0.1";
    private int port = 8000;
    private McpTransport transport = McpTransport.STDIO;

    /**
     * Returns the MCP host address.
     *
     * @return the host address
     */
    public String getHost() {
        return host;
    }

    /**
     * Sets the MCP host address.
     *
     * @param host the host address
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the MCP listen port.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the MCP listen port.
     *
     * @param port the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the MCP transport type.
     *
     * @return the transport
     */
    public McpTransport getTransport() {
        return transport;
    }

    /**
     * Sets the MCP transport from a string identifier.
     *
     * @param transport the transport name
     */
    @JsonProperty("transport")
    public void setTransport(String transport) {
        this.transport = McpTransport.fromString(transport);
    }
}
