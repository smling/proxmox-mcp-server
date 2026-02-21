package io.github.smling.proxmoxmcpserver.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads configuration from a JSON file on disk.
 */
public class ConfigLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    /**
     * Loads the configuration from the supplied path.
     *
     * @param configPath path to the JSON configuration file
     * @return the parsed configuration
     * @throws IllegalArgumentException when the path is missing or the file is invalid
     */
    public static Config loadConfig(String configPath) {
        if (configPath == null || configPath.isBlank()) {
            IllegalArgumentException ex = new IllegalArgumentException(
                "PROXMOX_MCP_CONFIG environment variable must be set"
            );
            logger.error("Configuration path is missing", ex);
            throw ex;
        }

        try {
            logger.info("Loading Proxmox MCP config from {}", configPath);
            String payload = Files.readString(Path.of(configPath));
            Config config = OBJECT_MAPPER.readValue(payload, Config.class);
            if (config.getProxmox() == null || config.getProxmox().getHost() == null
                || config.getProxmox().getHost().isBlank()) {
                IllegalArgumentException ex = new IllegalArgumentException("Proxmox host cannot be empty");
                logger.error("Invalid config: Proxmox host missing", ex);
                throw ex;
            }
            logger.info(
                "Loaded Proxmox config for host {}:{} (verifySsl={})",
                config.getProxmox().getHost(),
                config.getProxmox().getPort(),
                config.getProxmox().isVerifySsl()
            );
            return config;
        } catch (JsonProcessingException e) {
            logger.error("Invalid JSON in config file {}", configPath, e);
            throw new IllegalArgumentException("Invalid JSON in config file: " + e.getOriginalMessage());
        } catch (IOException e) {
            logger.error("Failed to read config file {}", configPath, e);
            throw new IllegalArgumentException("Failed to load config: " + e.getMessage());
        }
    }
}
