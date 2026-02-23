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

/**
 * MCP tool facade that delegates Proxmox operations to tool services.
 */
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

    /**
     * Creates the MCP tool facade with injected tool handlers.
     *
     * @param nodeTools node-related tools
     * @param vmTools VM-related tools
     * @param storageTools storage-related tools
     * @param clusterTools cluster-related tools
     * @param containerTools container-related tools
     * @param snapshotTools snapshot-related tools
     * @param isoTools ISO/template tools
     * @param backupTools backup tools
     */
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

    /**
     * Lists cluster nodes with status and resource usage.
     *
     * @return formatted node list
     */
    @Tool(name = "get_nodes", description = ToolDescriptions.GET_NODES_DESC)
    public String getNodes() {
        return nodeTools.getNodes();
    }

    /**
     * Retrieves detailed status for a single node.
     *
     * @param node the node name or ID
     * @return formatted node status
     */
    @Tool(name = "get_node_status", description = ToolDescriptions.GET_NODE_STATUS_DESC)
    public String getNodeStatus(@ToolParam(description = "Node name or ID") String node) {
        return nodeTools.getNodeStatus(required(node, "node"));
    }

    /**
     * Lists virtual machines across the cluster.
     *
     * @return formatted VM list
     */
    @Tool(name = "get_vms", description = ToolDescriptions.GET_VMS_DESC)
    public String getVms() {
        return vmTools.getVms();
    }

    /**
     * Creates a new virtual machine with the supplied configuration.
     *
     * @param node host node name
     * @param vmid new VM ID
     * @param name VM name
     * @param cpus CPU core count
     * @param memory memory size in MB
     * @param diskSize disk size in GB
     * @param storage storage pool name
     * @param ostype OS type
     * @param networkBridge network bridge name
     * @return creation status message
     */
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

    /**
     * Executes a shell command inside a VM using the guest agent.
     *
     * @param node host node name
     * @param vmid VM ID
     * @param command command to execute
     * @return formatted command output
     */
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

    /**
     * Starts a virtual machine.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return start status message
     */
    @Tool(name = "start_vm", description = ToolDescriptions.START_VM_DESC)
    public String startVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.startVm(required(node, "node"), required(vmid, "vmid"));
    }

    /**
     * Stops a virtual machine.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return stop status message
     */
    @Tool(name = "stop_vm", description = ToolDescriptions.STOP_VM_DESC)
    public String stopVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.stopVm(required(node, "node"), required(vmid, "vmid"));
    }

    /**
     * Shuts down a virtual machine gracefully.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return shutdown status message
     */
    @Tool(name = "shutdown_vm", description = ToolDescriptions.SHUTDOWN_VM_DESC)
    public String shutdownVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.shutdownVm(required(node, "node"), required(vmid, "vmid"));
    }

    /**
     * Resets a virtual machine.
     *
     * @param node host node name
     * @param vmid VM ID
     * @return reset status message
     */
    @Tool(name = "reset_vm", description = ToolDescriptions.RESET_VM_DESC)
    public String resetVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid
    ) {
        return vmTools.resetVm(required(node, "node"), required(vmid, "vmid"));
    }

    /**
     * Deletes a virtual machine.
     *
     * @param node host node name
     * @param vmid VM ID
     * @param force force deletion even if running
     * @return deletion status message
     */
    @Tool(name = "delete_vm", description = ToolDescriptions.DELETE_VM_DESC)
    public String deleteVm(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM ID") String vmid,
        @ToolParam(description = "Force delete even if running") Boolean force
    ) {
        boolean forceDelete = force != null && force;
        return vmTools.deleteVm(required(node, "node"), required(vmid, "vmid"), forceDelete);
    }

    /**
     * Lists storage pools and usage across the cluster.
     *
     * @return formatted storage list
     */
    @Tool(name = "get_storage", description = ToolDescriptions.GET_STORAGE_DESC)
    public String getStorage() {
        return storageTools.getStorage();
    }

    /**
     * Retrieves cluster status and quorum information.
     *
     * @return formatted cluster status
     */
    @Tool(name = "get_cluster_status", description = ToolDescriptions.GET_CLUSTER_STATUS_DESC)
    public String getClusterStatus() {
        return clusterTools.getClusterStatus();
    }

    /**
     * Lists containers with optional filters and format selection.
     *
     * @param node optional node filter
     * @param includeStats include live stats when true
     * @param includeRaw include raw payloads when true
     * @param formatStyle output format style
     * @return formatted container list
     */
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

    /**
     * Starts one or more containers based on a selector.
     *
     * @param selector container selector
     * @param formatStyle output format style
     * @return formatted action result
     */
    @Tool(name = "start_container", description = ToolDescriptions.START_CONTAINER_DESC)
    public String startContainer(
        @ToolParam(description = "Container selector") String selector,
        @ToolParam(description = "Output format: pretty|json (default: pretty)") String formatStyle
    ) {
        return containerTools.startContainer(required(selector, "selector"), defaultFormatStyle(formatStyle));
    }

    /**
     * Stops one or more containers.
     *
     * @param selector container selector
     * @param graceful whether to request shutdown
     * @param timeoutSeconds shutdown timeout
     * @param formatStyle output format style
     * @return formatted action result
     */
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

    /**
     * Restarts one or more containers.
     *
     * @param selector container selector
     * @param timeoutSeconds restart timeout
     * @param formatStyle output format style
     * @return formatted action result
     */
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

    /**
     * Updates resources for one or more containers.
     *
     * @param selector container selector
     * @param cores new CPU core count
     * @param memory new memory limit in MiB
     * @param swap new swap limit in MiB
     * @param diskGb additional disk size in GiB
     * @param disk disk identifier to resize
     * @param formatStyle output format style
     * @return formatted action result
     */
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

    /**
     * Creates a new LXC container.
     *
     * @param node host node name
     * @param vmid container ID
     * @param ostemplate OS template volume ID
     * @param hostname container hostname
     * @param cores CPU core count
     * @param memory memory limit in MiB
     * @param swap swap limit in MiB
     * @param diskSize disk size in GB
     * @param storage storage pool name
     * @param password root password
     * @param sshPublicKeys SSH public keys
     * @param networkBridge network bridge name
     * @param startAfterCreate whether to start after creation
     * @param unprivileged whether to create an unprivileged container
     * @return creation status message
     */
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

    /**
     * Deletes one or more containers.
     *
     * @param selector container selector
     * @param force force deletion even if running
     * @param formatStyle output format style
     * @return formatted action result
     */
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

    /**
     * Lists snapshots for a VM or container.
     *
     * @param node host node name
     * @param vmid VM or container ID
     * @param vmType VM type (qemu or lxc)
     * @return formatted snapshot list
     */
    @Tool(name = "list_snapshots", description = ToolDescriptions.LIST_SNAPSHOTS_DESC)
    public String listSnapshots(
        @ToolParam(description = "Host node name") String node,
        @ToolParam(description = "VM or container ID") String vmid,
        @ToolParam(description = "VM type: qemu or lxc (default: qemu)") String vmType
    ) {
        String vmTypeValue = (vmType == null || vmType.isBlank()) ? "qemu" : vmType;
        return snapshotTools.listSnapshots(required(node, "node"), required(vmid, "vmid"), vmTypeValue);
    }

    /**
     * Creates a snapshot for a VM or container.
     *
     * @param node host node name
     * @param vmid VM or container ID
     * @param snapname snapshot name
     * @param description snapshot description
     * @param vmstate include memory state for VMs
     * @param vmType VM type (qemu or lxc)
     * @return snapshot creation status
     */
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

    /**
     * Deletes a snapshot for a VM or container.
     *
     * @param node host node name
     * @param vmid VM or container ID
     * @param snapname snapshot name
     * @param vmType VM type (qemu or lxc)
     * @return snapshot deletion status
     */
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

    /**
     * Rolls back to a snapshot for a VM or container.
     *
     * @param node host node name
     * @param vmid VM or container ID
     * @param snapname snapshot name
     * @param vmType VM type (qemu or lxc)
     * @return rollback status message
     */
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

    /**
     * Lists ISO images in storage.
     *
     * @param node optional node filter
     * @param storage optional storage filter
     * @return formatted ISO list
     */
    @Tool(name = "list_isos", description = ToolDescriptions.LIST_ISOS_DESC)
    public String listIsos(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Optional storage filter") String storage
    ) {
        return isoTools.listIsos(node, storage);
    }

    /**
     * Lists OS templates in storage.
     *
     * @param node optional node filter
     * @param storage optional storage filter
     * @return formatted template list
     */
    @Tool(name = "list_templates", description = ToolDescriptions.LIST_TEMPLATES_DESC)
    public String listTemplates(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Optional storage filter") String storage
    ) {
        return isoTools.listTemplates(node, storage);
    }

    /**
     * Downloads an ISO image to Proxmox storage.
     *
     * @param node target node name
     * @param storage target storage pool
     * @param url ISO download URL
     * @param filename ISO filename
     * @param checksum optional checksum
     * @param checksumAlgorithm checksum algorithm
     * @return download status message
     */
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

    /**
     * Deletes an ISO image or template.
     *
     * @param node target node name
     * @param storage target storage pool
     * @param filename ISO or template filename
     * @return deletion status message
     */
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

    /**
     * Lists backups in Proxmox storage.
     *
     * @param node optional node filter
     * @param storage optional storage filter
     * @param vmid optional VM/container filter
     * @return formatted backup list
     */
    @Tool(name = "list_backups", description = ToolDescriptions.LIST_BACKUPS_DESC)
    public String listBackups(
        @ToolParam(description = "Optional node filter") String node,
        @ToolParam(description = "Optional storage filter") String storage,
        @ToolParam(description = "Optional VM/container ID filter") String vmid
    ) {
        return backupTools.listBackups(node, storage, vmid);
    }

    /**
     * Creates a backup for a VM or container.
     *
     * @param node target node name
     * @param vmid VM or container ID
     * @param storage target storage pool
     * @param compress compression mode
     * @param mode backup mode
     * @param notes optional notes
     * @return backup creation status
     */
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

    /**
     * Restores a VM or container from a backup.
     *
     * @param node target node name
     * @param archive backup archive volume ID
     * @param vmid new VM/container ID
     * @param storage target storage pool
     * @param unique whether to generate unique MACs
     * @return restore status message
     */
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

    /**
     * Deletes a backup volume.
     *
     * @param node target node name
     * @param storage target storage pool
     * @param volid backup volume ID
     * @return deletion status message
     */
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

    /**
     * Applies the default format style when none is provided.
     *
     * @param formatStyle requested format style
     * @return resolved format style
     */
    private String defaultFormatStyle(String formatStyle) {
        if (formatStyle == null || formatStyle.isBlank()) {
            return "pretty";
        }
        return formatStyle;
    }

    /**
     * Ensures a required string parameter is provided.
     *
     * @param value the value to validate
     * @param fieldName the field name for error messages
     * @return the validated value
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
