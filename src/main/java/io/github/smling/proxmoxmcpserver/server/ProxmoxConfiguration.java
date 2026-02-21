package io.github.smling.proxmoxmcpserver.server;

import io.github.smling.proxmoxmcpserver.config.Config;
import io.github.smling.proxmoxmcpserver.config.ConfigLoader;
import io.github.smling.proxmoxmcpserver.core.ProxmoxManager;
import io.github.smling.proxmoxmcpserver.tools.BackupTools;
import io.github.smling.proxmoxmcpserver.tools.ClusterTools;
import io.github.smling.proxmoxmcpserver.tools.ContainerTools;
import io.github.smling.proxmoxmcpserver.tools.IsoTools;
import io.github.smling.proxmoxmcpserver.tools.NodeTools;
import io.github.smling.proxmoxmcpserver.tools.SnapshotTools;
import io.github.smling.proxmoxmcpserver.tools.StorageTools;
import io.github.smling.proxmoxmcpserver.tools.VmTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxmoxConfiguration {
    @Bean
    public Config proxmoxConfig() {
        String configPath = System.getenv("PROXMOX_MCP_CONFIG");
        return ConfigLoader.loadConfig(configPath);
    }

    @Bean
    public ProxmoxManager proxmoxManager(Config config) {
        return new ProxmoxManager(config.getProxmox(), config.getAuth());
    }

    @Bean
    public NodeTools nodeTools(ProxmoxManager manager) {
        return new NodeTools(manager.getApi());
    }

    @Bean
    public VmTools vmTools(ProxmoxManager manager) {
        return new VmTools(manager.getApi());
    }

    @Bean
    public StorageTools storageTools(ProxmoxManager manager) {
        return new StorageTools(manager.getApi());
    }

    @Bean
    public ClusterTools clusterTools(ProxmoxManager manager) {
        return new ClusterTools(manager.getApi());
    }

    @Bean
    public ContainerTools containerTools(ProxmoxManager manager) {
        return new ContainerTools(manager.getApi());
    }

    @Bean
    public SnapshotTools snapshotTools(ProxmoxManager manager) {
        return new SnapshotTools(manager.getApi());
    }

    @Bean
    public IsoTools isoTools(ProxmoxManager manager) {
        return new IsoTools(manager.getApi());
    }

    @Bean
    public BackupTools backupTools(ProxmoxManager manager) {
        return new BackupTools(manager.getApi());
    }
}
