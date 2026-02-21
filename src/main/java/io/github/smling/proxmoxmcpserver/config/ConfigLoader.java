package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads configuration from a JSON file on disk.
 */
public class ConfigLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Loads the configuration from the supplied path.
     *
     * @param configPath path to the JSON configuration file
     * @return the parsed configuration
     * @throws IllegalArgumentException when the path is missing or the file is invalid
     */
    public static Config loadConfig(String configPath) {
        if (configPath == null || configPath.isBlank()) {
            throw new IllegalArgumentException("PROXMOX_MCP_CONFIG environment variable must be set");
        }

        try {
            String payload = Files.readString(Path.of(configPath));
            Config config = OBJECT_MAPPER.readValue(payload, Config.class);
            if (config.getProxmox() == null || config.getProxmox().getHost() == null
                || config.getProxmox().getHost().isBlank()) {
                throw new IllegalArgumentException("Proxmox host cannot be empty");
            }
            return config;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON in config file: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load config: " + e.getMessage());
        }
    }
}
