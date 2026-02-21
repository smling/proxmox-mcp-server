package io.github.smling.proxmoxmcpserver.spring;

import io.github.smling.proxmoxmcpserver.config.Config;
import io.github.smling.proxmoxmcpserver.config.ConfigLoader;
import io.github.smling.proxmoxmcpserver.core.ProxmoxManager;
import io.github.smling.proxmoxmcpserver.mcp.ProxmoxMcpTools;
import io.github.smling.proxmoxmcpserver.tools.BackupTools;
import io.github.smling.proxmoxmcpserver.tools.ClusterTools;
import io.github.smling.proxmoxmcpserver.tools.ContainerTools;
import io.github.smling.proxmoxmcpserver.tools.IsoTools;
import io.github.smling.proxmoxmcpserver.tools.NodeTools;
import io.github.smling.proxmoxmcpserver.tools.SnapshotTools;
import io.github.smling.proxmoxmcpserver.tools.StorageTools;
import io.github.smling.proxmoxmcpserver.tools.VmTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires Proxmox clients and MCP tools.
 */
@Configuration
public class ProxmoxConfiguration {
    /**
     * Loads the Proxmox MCP configuration from the environment-configured file.
     *
     * @return the application configuration
     */
    @Bean
    public Config proxmoxConfig() {
        String configPath = System.getenv("PROXMOX_MCP_CONFIG");
        return ConfigLoader.loadConfig(configPath);
    }

    /**
     * Builds the Proxmox manager that validates the API connection.
     *
     * @param config the parsed configuration
     * @return the manager instance
     */
    @Bean
    public ProxmoxManager proxmoxManager(Config config) {
        return new ProxmoxManager(config.getProxmox(), config.getAuth());
    }

    /**
     * Creates the node tools bean.
     *
     * @param manager the Proxmox manager
     * @return node tools
     */
    @Bean
    public NodeTools nodeTools(ProxmoxManager manager) {
        return new NodeTools(manager.getApi());
    }

    /**
     * Creates the VM tools bean.
     *
     * @param manager the Proxmox manager
     * @return VM tools
     */
    @Bean
    public VmTools vmTools(ProxmoxManager manager) {
        return new VmTools(manager.getApi());
    }

    /**
     * Creates the storage tools bean.
     *
     * @param manager the Proxmox manager
     * @return storage tools
     */
    @Bean
    public StorageTools storageTools(ProxmoxManager manager) {
        return new StorageTools(manager.getApi());
    }

    /**
     * Creates the cluster tools bean.
     *
     * @param manager the Proxmox manager
     * @return cluster tools
     */
    @Bean
    public ClusterTools clusterTools(ProxmoxManager manager) {
        return new ClusterTools(manager.getApi());
    }

    /**
     * Creates the container tools bean.
     *
     * @param manager the Proxmox manager
     * @return container tools
     */
    @Bean
    public ContainerTools containerTools(ProxmoxManager manager) {
        return new ContainerTools(manager.getApi());
    }

    /**
     * Creates the snapshot tools bean.
     *
     * @param manager the Proxmox manager
     * @return snapshot tools
     */
    @Bean
    public SnapshotTools snapshotTools(ProxmoxManager manager) {
        return new SnapshotTools(manager.getApi());
    }

    /**
     * Creates the ISO/template tools bean.
     *
     * @param manager the Proxmox manager
     * @return ISO tools
     */
    @Bean
    public IsoTools isoTools(ProxmoxManager manager) {
        return new IsoTools(manager.getApi());
    }

    /**
     * Creates the backup tools bean.
     *
     * @param manager the Proxmox manager
     * @return backup tools
     */
    @Bean
    public BackupTools backupTools(ProxmoxManager manager) {
        return new BackupTools(manager.getApi());
    }

    /**
     * Registers tool callbacks for MCP.
     *
     * @param proxmoxMcpTools the tool facade
     * @return the callback provider
     */
    @Bean
    public ToolCallbackProvider proxmoxToolCallbacks(ProxmoxMcpTools proxmoxMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(proxmoxMcpTools).build();
    }
}
