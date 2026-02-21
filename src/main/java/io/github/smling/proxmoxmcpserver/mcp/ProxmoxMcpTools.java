package io.github.smling.proxmoxmcpserver.mcp;

import io.github.smling.proxmoxmcpserver.tools.BackupTools;
import io.github.smling.proxmoxmcpserver.tools.ClusterTools;
import io.github.smling.proxmoxmcpserver.tools.ContainerTools;
import io.github.smling.proxmoxmcpserver.tools.IsoTools;
import io.github.smling.proxmoxmcpserver.tools.NodeTools;
import io.github.smling.proxmoxmcpserver.tools.SnapshotTools;
import io.github.smling.proxmoxmcpserver.tools.StorageTools;
import io.github.smling.proxmoxmcpserver.tools.VmTools;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class ProxmoxMcpTools {
    private final NodeTools nodeTools;
    private final VmTools vmTools;
    private final StorageTools storageTools;
    private final ClusterTools clusterTools;
    private final ContainerTools containerTools;
    private final SnapshotTools snapshotTools;
    private final IsoTools isoTools;
    private final BackupTools backupTools;

    public ProxmoxMcpTools(
        NodeTools nodeTools,
        VmTools vmTools,
        StorageTools storageTools,
        ClusterTools clusterTools,
        ContainerTools containerTools,
        SnapshotTools snapshotTools,
        IsoTools isoTools,
        BackupTools backupTools
    ) {
        this.nodeTools = nodeTools;
        this.vmTools = vmTools;
        this.storageTools = storageTools;
        this.clusterTools = clusterTools;
        this.containerTools = containerTools;
        this.snapshotTools = snapshotTools;
        this.isoTools = isoTools;
        this.backupTools = backupTools;
    }

    @Tool(name = "get_nodes", description = ToolDescriptions.GET_NODES_DESC)
    public String getNodes() {
        return nodeTools.getNodes();
    }

    @Tool(name = "get_node_status", description = ToolDescriptions.GET_NODE_STATUS_DESC)
    public String getNodeStatus(@ToolParam(description = "Node name or ID") String node) {
        return nodeTools.getNodeStatus(required(node, "node"));
    }

    @Tool(name = "get_vms", description = ToolDescriptions.GET_VMS_DESC)
    public String getVms() {
        return vmTools.getVms();
    }

    @Tool(name = "create_vm", description = ToolDescriptions.CREATE_VM_DESC)
    public String createVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "New VM ID") String vmid,
        @ToolParam(description = "VM name") String name,
        @ToolParam(description = "Number of CPU cores") int cpus,
        @ToolParam(description = "Memory size in MB") int memory,
        @ToolParam(description = "Disk size in GB") int diskSize,
        @ToolParam(description = "Storage pool name") String storage,
        @ToolParam(description = "OS type (default: l26)") String ostype,
        @ToolParam(description = "Network bridge (default: vmbr0)") String networkBridge
    ) {
        return vmTools.createVm(
            required(node, "node"),
            required(vmid, "vmid"),
            required(name, "name"),
            cpus,
            memory,
            diskSize,
            storage,
            ostype,
            networkBridge
        );
    }

    @Tool(name = "execute_vm_command", description = ToolDescriptions.EXECUTE_VM_COMMAND_DESC)
    public String executeVmCommand(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid,
        @ToolParam(description = "Shell command to execute") String command
    ) {
        return vmTools.executeCommand(
            required(node, "node"),
            required(vmid, "vmid"),
            required(command, "command")
        );
    }

    @Tool(name = "start_vm", description = ToolDescriptions.START_VM_DESC)
    public String startVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.startVm(required(node, "node"), required(vmid, "vmid"));
    }

    @Tool(name = "stop_vm", description = ToolDescriptions.STOP_VM_DESC)
    public String stopVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.stopVm(required(node, "node"), required(vmid, "vmid"));
    }

    @Tool(name = "shutdown_vm", description = ToolDescriptions.SHUTDOWN_VM_DESC)
    public String shutdownVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.shutdownVm(required(node, "node"), required(vmid, "vmid"));
    }

    @Tool(name = "reset_vm", description = ToolDescriptions.RESET_VM_DESC)
    public String resetVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.resetVm(required(node, "node"), required(vmid, "vmid"));
    }

    @Tool(name = "delete_vm", description = ToolDescriptions.DELETE_VM_DESC)
    public String deleteVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid,
        @ToolParam(description = "Force delete even if running") Boolean force
    ) {
        boolean forceDelete = force != null && force;
        return vmTools.deleteVm(required(node, "node"), required(vmid, "vmid"), forceDelete);
    }

    @Tool(name = "get_storage", description = ToolDescriptions.GET_STORAGE_DESC)
    public String getStorage() {
        return storageTools.getStorage();
    }

    @Tool(name = "get_cluster_status", description = ToolDescriptions.GET_CLUSTER_STATUS_DESC)
    public String getClusterStatus() {
        return clusterTools.getClusterStatus();
    }

    @Tool(name = "get_containers", description = ToolDescriptions.GET_CONTAINERS_DESC)
    public String getContainers(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Include live CPU/memory stats (default: true)") Boolean includeStats,
        @ToolParam(description = "Include raw API payloads (default: false)") Boolean includeRaw,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        boolean includeStatsValue = includeStats == null || includeStats;
        boolean includeRawValue = includeRaw != null && includeRaw;
        String formatStyleValue = defaultFormatStyle(formatStyle);
        return containerTools.getContainers(node, includeStatsValue, includeRawValue, formatStyleValue);
    }

    @Tool(name = "start_container", description = ToolDescriptions.START_CONTAINER_DESC)
    public String startContainer(
        @ToolParam(description = "Container selector") String selector,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        return containerTools.startContainer(required(selector, "selector"), defaultFormatStyle(formatStyle));
    }

    @Tool(name = "stop_container", description = ToolDescriptions.STOP_CONTAINER_DESC)
    public String stopContainer(
        @ToolParam(description = "Container selector") String selector,
        @ToolParam(description = "Use graceful shutdown (default: true)") Boolean graceful,
        @ToolParam(description = "Shutdown timeout seconds (default: 10)") Integer timeoutSeconds,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        boolean gracefulValue = graceful == null || graceful;
        int timeoutValue = timeoutSeconds == null ? 10 : timeoutSeconds;
        return containerTools.stopContainer(
            required(selector, "selector"),
            gracefulValue,
            timeoutValue,
            defaultFormatStyle(formatStyle)
        );
    }

    @Tool(name = "restart_container", description = ToolDescriptions.RESTART_CONTAINER_DESC)
    public String restartContainer(
        @ToolParam(description = "Container selector") String selector,
        @ToolParam(description = "Restart timeout seconds (default: 10)") Integer timeoutSeconds,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        int timeoutValue = timeoutSeconds == null ? 10 : timeoutSeconds;
        return containerTools.restartContainer(
            required(selector, "selector"),
            timeoutValue,
            defaultFormatStyle(formatStyle)
        );
    }

    @Tool(name = "update_container_resources", description = ToolDescriptions.UPDATE_CONTAINER_RESOURCES_DESC)
    public String updateContainerResources(
        @ToolParam(description = "Container selector") String selector,
        @ToolParam(description = "New CPU core count") Integer cores,
        @ToolParam(description = "New memory limit in MiB") Integer memory,
        @ToolParam(description = "New swap limit in MiB") Integer swap,
        @ToolParam(description = "Additional disk size in GiB") Integer diskGb,
        @ToolParam(description = "Disk identifier to resize (default: rootfs)") String disk,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        String diskValue = (disk == null || disk.isBlank()) ? "rootfs" : disk;
        return containerTools.updateContainerResources(
            required(selector, "selector"),
            cores,
            memory,
            swap,
            diskGb,
            diskValue,
            defaultFormatStyle(formatStyle)
        );
    }

    @Tool(name = "create_container", description = ToolDescriptions.CREATE_CONTAINER_DESC)
    public String createContainer(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "Container ID") String vmid,
        @ToolParam(description = "OS template volume ID") String ostemplate,
        @ToolParam(description = "Container hostname") String hostname,
        @ToolParam(description = "CPU cores (default: 1)") Integer cores,
        @ToolParam(description = "Memory MiB (default: 512)") Integer memory,
        @ToolParam(description = "Swap MiB (default: 512)") Integer swap,
        @ToolParam(description = "Disk size GB (default: 8)") Integer diskSize,
        @ToolParam(description = "Storage pool name") String storage,
        @ToolParam(description = "Root password") String password,
        @ToolParam(description = "SSH public keys") String sshPublicKeys,
        @ToolParam(description = "Network bridge (default: vmbr0)") String networkBridge,
        @ToolParam(description = "Start after create (default: false)") Boolean startAfterCreate,
        @ToolParam(description = "Create unprivileged container (default: true)") Boolean unprivileged
    ) {
        int coresValue = cores == null ? 1 : cores;
        int memoryValue = memory == null ? 512 : memory;
        int swapValue = swap == null ? 512 : swap;
        int diskSizeValue = diskSize == null ? 8 : diskSize;
        String bridgeValue = (networkBridge == null || networkBridge.isBlank()) ? "vmbr0" : networkBridge;
        boolean startAfterCreateValue = startAfterCreate != null && startAfterCreate;
        boolean unprivilegedValue = unprivileged == null || unprivileged;

        return containerTools.createContainer(
            required(node, "node"),
            required(vmid, "vmid"),
            required(ostemplate, "ostemplate"),
            hostname,
            coresValue,
            memoryValue,
            swapValue,
            diskSizeValue,
            storage,
            password,
            sshPublicKeys,
            bridgeValue,
            startAfterCreateValue,
            unprivilegedValue
        );
    }

    @Tool(name = "delete_container", description = ToolDescriptions.DELETE_CONTAINER_DESC)
    public String deleteContainer(
        @ToolParam(description = "Container selector") String selector,
        @ToolParam(description = "Force delete even if running") Boolean force,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        boolean forceDelete = force != null && force;
        return containerTools.deleteContainer(
            required(selector, "selector"),
            forceDelete,
            defaultFormatStyle(formatStyle)
        );
    }

    @Tool(name = "list_snapshots", description = ToolDescriptions.LIST_SNAPSHOTS_DESC)
    public String listSnapshots(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM or container ID") String vmid,
        @ToolParam(description = "VM type: qemu or lxc (default: qemu)") String vmType
    ) {
        String vmTypeValue = (vmType == null || vmType.isBlank()) ? "qemu" : vmType;
        return snapshotTools.listSnapshots(required(node, "node"), required(vmid, "vmid"), vmTypeValue);
    }

    @Tool(name = "create_snapshot", description = ToolDescriptions.CREATE_SNAPSHOT_DESC)
    public String createSnapshot(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM or container ID") String vmid,
        @ToolParam(description = "Snapshot name") String snapname,
        @ToolParam(description = "Snapshot description") String description,
        @ToolParam(description = "Include VM memory state (default: false)") Boolean vmstate,
        @ToolParam(description = "VM type: qemu or lxc (default: qemu)") String vmType
    ) {
        String vmTypeValue = (vmType == null || vmType.isBlank()) ? "qemu" : vmType;
        boolean vmstateValue = vmstate != null && vmstate;
        return snapshotTools.createSnapshot(
            required(node, "node"),
            required(vmid, "vmid"),
            required(snapname, "snapname"),
            description,
            vmstateValue,
            vmTypeValue
        );
    }

    @Tool(name = "delete_snapshot", description = ToolDescriptions.DELETE_SNAPSHOT_DESC)
    public String deleteSnapshot(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM or container ID") String vmid,
        @ToolParam(description = "Snapshot name") String snapname,
        @ToolParam(description = "VM type: qemu or lxc (default: qemu)") String vmType
    ) {
        String vmTypeValue = (vmType == null || vmType.isBlank()) ? "qemu" : vmType;
        return snapshotTools.deleteSnapshot(
            required(node, "node"),
            required(vmid, "vmid"),
            required(snapname, "snapname"),
            vmTypeValue
        );
    }

    @Tool(name = "rollback_snapshot", description = ToolDescriptions.ROLLBACK_SNAPSHOT_DESC)
    public String rollbackSnapshot(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM or container ID") String vmid,
        @ToolParam(description = "Snapshot name") String snapname,
        @ToolParam(description = "VM type: qemu or lxc (default: qemu)") String vmType
    ) {
        String vmTypeValue = (vmType == null || vmType.isBlank()) ? "qemu" : vmType;
        return snapshotTools.rollbackSnapshot(
            required(node, "node"),
            required(vmid, "vmid"),
            required(snapname, "snapname"),
            vmTypeValue
        );
    }

    @Tool(name = "list_isos", description = ToolDescriptions.LIST_ISOS_DESC)
    public String listIsos(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Optional storage filter") String storage
    ) {
        return isoTools.listIsos(node, storage);
    }

    @Tool(name = "list_templates", description = ToolDescriptions.LIST_TEMPLATES_DESC)
    public String listTemplates(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Optional storage filter") String storage
    ) {
        return isoTools.listTemplates(node, storage);
    }

    @Tool(name = "download_iso", description = ToolDescriptions.DOWNLOAD_ISO_DESC)
    public String downloadIso(
        @ToolParam(description = "Target node name") String node,
        @ToolParam(description = "Target storage pool") String storage,
        @ToolParam(description = "ISO download URL") String url,
        @ToolParam(description = "ISO filename") String filename,
        @ToolParam(description = "Optional checksum") String checksum,
        @ToolParam(description = "Checksum algorithm (default: sha256)") String checksumAlgorithm
    ) {
        String algorithm = (checksumAlgorithm == null || checksumAlgorithm.isBlank()) ? "sha256" : checksumAlgorithm;
        return isoTools.downloadIso(
            required(node, "node"),
            required(storage, "storage"),
            required(url, "url"),
            required(filename, "filename"),
            checksum,
            algorithm
        );
    }

    @Tool(name = "delete_iso", description = ToolDescriptions.DELETE_ISO_DESC)
    public String deleteIso(
        @ToolParam(description = "Target node name") String node,
        @ToolParam(description = "Target storage pool") String storage,
        @ToolParam(description = "ISO/template filename") String filename
    ) {
        return isoTools.deleteIso(
            required(node, "node"),
            required(storage, "storage"),
            required(filename, "filename")
        );
    }

    @Tool(name = "list_backups", description = ToolDescriptions.LIST_BACKUPS_DESC)
    public String listBackups(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Optional storage filter") String storage,
        @ToolParam(description = "Optional VM/container ID filter") String vmid
    ) {
        return backupTools.listBackups(node, storage, vmid);
    }

    @Tool(name = "create_backup", description = ToolDescriptions.CREATE_BACKUP_DESC)
    public String createBackup(
        @ToolParam(description = "Target node name") String node,
        @ToolParam(description = "VM or container ID") String vmid,
        @ToolParam(description = "Target storage pool") String storage,
        @ToolParam(description = "Compression (default: zstd)") String compress,
        @ToolParam(description = "Backup mode (default: snapshot)") String mode,
        @ToolParam(description = "Notes/description") String notes
    ) {
        return backupTools.createBackup(
            required(node, "node"),
            required(vmid, "vmid"),
            required(storage, "storage"),
            compress,
            mode,
            notes
        );
    }

    @Tool(name = "restore_backup", description = ToolDescriptions.RESTORE_BACKUP_DESC)
    public String restoreBackup(
        @ToolParam(description = "Target node name") String node,
        @ToolParam(description = "Backup archive volume ID") String archive,
        @ToolParam(description = "New VM/container ID") String vmid,
        @ToolParam(description = "Target storage pool") String storage,
        @ToolParam(description = "Generate unique MACs (default: true)") Boolean unique
    ) {
        boolean uniqueValue = unique == null || unique;
        return backupTools.restoreBackup(
            required(node, "node"),
            required(archive, "archive"),
            required(vmid, "vmid"),
            storage,
            uniqueValue
        );
    }

    @Tool(name = "delete_backup", description = ToolDescriptions.DELETE_BACKUP_DESC)
    public String deleteBackup(
        @ToolParam(description = "Target node name") String node,
        @ToolParam(description = "Target storage pool") String storage,
        @ToolParam(description = "Backup volume ID") String volid
    ) {
        return backupTools.deleteBackup(
            required(node, "node"),
            required(storage, "storage"),
            required(volid, "volid")
        );
    }

    private String defaultFormatStyle(String formatStyle) {
        if (formatStyle == null || formatStyle.isBlank()) {
            return "pretty";
        }
        return formatStyle;
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
