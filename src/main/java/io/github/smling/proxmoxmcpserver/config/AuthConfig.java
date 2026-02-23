package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Authentication configuration for Proxmox API access.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthConfig {
    private String user;
    @JsonProperty("token_name")
    private String tokenName;
    @JsonProperty("token_value")
    private String tokenValue;

    /**
     * Returns the Proxmox API user identifier.
     *
     * @return the API user
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the Proxmox API user identifier.
     *
     * @param user the API user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the API token name.
     *
     * @return the token name
     */
    public String getTokenName() {
        return tokenName;
    }

    /**
     * Sets the API token name.
     *
     * @param tokenName the token name
     */
    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    /**
     * Returns the API token value.
     *
     * @return the token value
     */
    public String getTokenValue() {
        return tokenValue;
    }

    /**
     * Sets the API token value.
     *
     * @param tokenValue the token value
     */
    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }
}
