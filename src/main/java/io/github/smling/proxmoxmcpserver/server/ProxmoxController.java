package io.github.smling.proxmoxmcpserver.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.smling.proxmoxmcpserver.tools.BackupTools;
import io.github.smling.proxmoxmcpserver.tools.ClusterTools;
import io.github.smling.proxmoxmcpserver.tools.ContainerTools;
import io.github.smling.proxmoxmcpserver.tools.IsoTools;
import io.github.smling.proxmoxmcpserver.tools.NodeTools;
import io.github.smling.proxmoxmcpserver.tools.SnapshotTools;
import io.github.smling.proxmoxmcpserver.tools.StorageTools;
import io.github.smling.proxmoxmcpserver.tools.VmTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProxmoxController {
    private final NodeTools nodeTools;
    private final VmTools vmTools;
    private final StorageTools storageTools;
    private final ClusterTools clusterTools;
    private final ContainerTools containerTools;
    private final SnapshotTools snapshotTools;
    private final IsoTools isoTools;
    private final BackupTools backupTools;

    public ProxmoxController(
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

    @PostMapping(value = "/get_nodes", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getNodes() {
        return nodeTools.getNodes();
    }

    @PostMapping(value = "/get_node_status", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getNodeStatus(@RequestBody NodeRequest request) {
        return nodeTools.getNodeStatus(required(request.node(), "node"));
    }

    @PostMapping(value = "/get_vms", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getVms() {
        return vmTools.getVms();
    }

    @PostMapping(value = "/create_vm", produces = MediaType.TEXT_PLAIN_VALUE)
    public String createVm(@RequestBody CreateVmRequest request) {
        return vmTools.createVm(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.name(), "name"),
            request.cpus(),
            request.memory(),
            request.diskSize(),
            request.storage(),
            request.ostype(),
            request.networkBridge()
        );
    }

    @PostMapping(value = "/execute_vm_command", produces = MediaType.TEXT_PLAIN_VALUE)
    public String executeVmCommand(@RequestBody VmCommandRequest request) {
        return vmTools.executeCommand(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.command(), "command")
        );
    }

    @PostMapping(value = "/start_vm", produces = MediaType.TEXT_PLAIN_VALUE)
    public String startVm(@RequestBody VmActionRequest request) {
        return vmTools.startVm(required(request.node(), "node"), required(request.vmid(), "vmid"));
    }

    @PostMapping(value = "/stop_vm", produces = MediaType.TEXT_PLAIN_VALUE)
    public String stopVm(@RequestBody VmActionRequest request) {
        return vmTools.stopVm(required(request.node(), "node"), required(request.vmid(), "vmid"));
    }

    @PostMapping(value = "/shutdown_vm", produces = MediaType.TEXT_PLAIN_VALUE)
    public String shutdownVm(@RequestBody VmActionRequest request) {
        return vmTools.shutdownVm(required(request.node(), "node"), required(request.vmid(), "vmid"));
    }

    @PostMapping(value = "/reset_vm", produces = MediaType.TEXT_PLAIN_VALUE)
    public String resetVm(@RequestBody VmActionRequest request) {
        return vmTools.resetVm(required(request.node(), "node"), required(request.vmid(), "vmid"));
    }

    @PostMapping(value = "/delete_vm", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteVm(@RequestBody DeleteVmRequest request) {
        return vmTools.deleteVm(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            request.force() != null && request.force()
        );
    }

    @PostMapping(value = "/get_storage", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getStorage() {
        return storageTools.getStorage();
    }

    @PostMapping(value = "/get_cluster_status", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getClusterStatus() {
        return clusterTools.getClusterStatus();
    }

    @PostMapping(value = "/get_containers", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getContainers(@RequestBody ContainerListRequest request) {
        boolean includeStats = request.includeStats() == null || request.includeStats();
        boolean includeRaw = request.includeRaw() != null && request.includeRaw();
        String formatStyle = request.formatStyle() == null ? "pretty" : request.formatStyle();
        return containerTools.getContainers(request.node(), includeStats, includeRaw, formatStyle);
    }

    @PostMapping(value = "/start_container", produces = MediaType.TEXT_PLAIN_VALUE)
    public String startContainer(@RequestBody ContainerSelectorRequest request) {
        String formatStyle = request.formatStyle() == null ? "pretty" : request.formatStyle();
        return containerTools.startContainer(required(request.selector(), "selector"), formatStyle);
    }

    @PostMapping(value = "/stop_container", produces = MediaType.TEXT_PLAIN_VALUE)
    public String stopContainer(@RequestBody StopContainerRequest request) {
        boolean graceful = request.graceful() == null || request.graceful();
        int timeoutSeconds = request.timeoutSeconds() == null ? 10 : request.timeoutSeconds();
        String formatStyle = request.formatStyle() == null ? "pretty" : request.formatStyle();
        return containerTools.stopContainer(required(request.selector(), "selector"), graceful, timeoutSeconds, formatStyle);
    }

    @PostMapping(value = "/restart_container", produces = MediaType.TEXT_PLAIN_VALUE)
    public String restartContainer(@RequestBody RestartContainerRequest request) {
        int timeoutSeconds = request.timeoutSeconds() == null ? 10 : request.timeoutSeconds();
        String formatStyle = request.formatStyle() == null ? "pretty" : request.formatStyle();
        return containerTools.restartContainer(required(request.selector(), "selector"), timeoutSeconds, formatStyle);
    }

    @PostMapping(value = "/update_container_resources", produces = MediaType.TEXT_PLAIN_VALUE)
    public String updateContainerResources(@RequestBody UpdateContainerRequest request) {
        String formatStyle = request.formatStyle() == null ? "pretty" : request.formatStyle();
        String disk = request.disk() == null ? "rootfs" : request.disk();
        return containerTools.updateContainerResources(
            required(request.selector(), "selector"),
            request.cores(),
            request.memory(),
            request.swap(),
            request.diskGb(),
            disk,
            formatStyle
        );
    }

    @PostMapping(value = "/create_container", produces = MediaType.TEXT_PLAIN_VALUE)
    public String createContainer(@RequestBody CreateContainerRequest request) {
        return containerTools.createContainer(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.ostemplate(), "ostemplate"),
            request.hostname(),
            request.cores() == null ? 1 : request.cores(),
            request.memory() == null ? 512 : request.memory(),
            request.swap() == null ? 512 : request.swap(),
            request.diskSize() == null ? 8 : request.diskSize(),
            request.storage(),
            request.password(),
            request.sshPublicKeys(),
            request.networkBridge() == null ? "vmbr0" : request.networkBridge(),
            request.startAfterCreate() != null && request.startAfterCreate(),
            request.unprivileged() == null || request.unprivileged()
        );
    }

    @PostMapping(value = "/delete_container", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteContainer(@RequestBody DeleteContainerRequest request) {
        String formatStyle = request.formatStyle() == null ? "pretty" : request.formatStyle();
        boolean force = request.force() != null && request.force();
        return containerTools.deleteContainer(required(request.selector(), "selector"), force, formatStyle);
    }

    @PostMapping(value = "/list_snapshots", produces = MediaType.TEXT_PLAIN_VALUE)
    public String listSnapshots(@RequestBody SnapshotRequest request) {
        String vmType = request.vmType() == null ? "qemu" : request.vmType();
        return snapshotTools.listSnapshots(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            vmType
        );
    }

    @PostMapping(value = "/create_snapshot", produces = MediaType.TEXT_PLAIN_VALUE)
    public String createSnapshot(@RequestBody CreateSnapshotRequest request) {
        String vmType = request.vmType() == null ? "qemu" : request.vmType();
        boolean vmstate = request.vmstate() != null && request.vmstate();
        return snapshotTools.createSnapshot(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.snapname(), "snapname"),
            request.description(),
            vmstate,
            vmType
        );
    }

    @PostMapping(value = "/delete_snapshot", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteSnapshot(@RequestBody SnapshotDeleteRequest request) {
        String vmType = request.vmType() == null ? "qemu" : request.vmType();
        return snapshotTools.deleteSnapshot(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.snapname(), "snapname"),
            vmType
        );
    }

    @PostMapping(value = "/rollback_snapshot", produces = MediaType.TEXT_PLAIN_VALUE)
    public String rollbackSnapshot(@RequestBody SnapshotDeleteRequest request) {
        String vmType = request.vmType() == null ? "qemu" : request.vmType();
        return snapshotTools.rollbackSnapshot(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.snapname(), "snapname"),
            vmType
        );
    }

    @PostMapping(value = "/list_isos", produces = MediaType.TEXT_PLAIN_VALUE)
    public String listIsos(@RequestBody IsoListRequest request) {
        return isoTools.listIsos(request.node(), request.storage());
    }

    @PostMapping(value = "/list_templates", produces = MediaType.TEXT_PLAIN_VALUE)
    public String listTemplates(@RequestBody IsoListRequest request) {
        return isoTools.listTemplates(request.node(), request.storage());
    }

    @PostMapping(value = "/download_iso", produces = MediaType.TEXT_PLAIN_VALUE)
    public String downloadIso(@RequestBody IsoDownloadRequest request) {
        String algorithm = request.checksumAlgorithm() == null ? "sha256" : request.checksumAlgorithm();
        return isoTools.downloadIso(
            required(request.node(), "node"),
            required(request.storage(), "storage"),
            required(request.url(), "url"),
            required(request.filename(), "filename"),
            request.checksum(),
            algorithm
        );
    }

    @PostMapping(value = "/delete_iso", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteIso(@RequestBody IsoDeleteRequest request) {
        return isoTools.deleteIso(
            required(request.node(), "node"),
            required(request.storage(), "storage"),
            required(request.filename(), "filename")
        );
    }

    @PostMapping(value = "/list_backups", produces = MediaType.TEXT_PLAIN_VALUE)
    public String listBackups(@RequestBody BackupListRequest request) {
        return backupTools.listBackups(request.node(), request.storage(), request.vmid());
    }

    @PostMapping(value = "/create_backup", produces = MediaType.TEXT_PLAIN_VALUE)
    public String createBackup(@RequestBody CreateBackupRequest request) {
        return backupTools.createBackup(
            required(request.node(), "node"),
            required(request.vmid(), "vmid"),
            required(request.storage(), "storage"),
            request.compress(),
            request.mode(),
            request.notes()
        );
    }

    @PostMapping(value = "/restore_backup", produces = MediaType.TEXT_PLAIN_VALUE)
    public String restoreBackup(@RequestBody RestoreBackupRequest request) {
        boolean unique = request.unique() == null || request.unique();
        return backupTools.restoreBackup(
            required(request.node(), "node"),
            required(request.archive(), "archive"),
            required(request.vmid(), "vmid"),
            request.storage(),
            unique
        );
    }

    @PostMapping(value = "/delete_backup", produces = MediaType.TEXT_PLAIN_VALUE)
    public String deleteBackup(@RequestBody DeleteBackupRequest request) {
        return backupTools.deleteBackup(
            required(request.node(), "node"),
            required(request.storage(), "storage"),
            required(request.volid(), "volid")
        );
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private record NodeRequest(String node) {
    }

    private record VmActionRequest(String node, String vmid) {
    }

    private record DeleteVmRequest(String node, String vmid, Boolean force) {
    }

    private record VmCommandRequest(String node, String vmid, String command) {
    }

    private record CreateVmRequest(
        String node,
        String vmid,
        String name,
        int cpus,
        int memory,
        @JsonProperty("disk_size") int diskSize,
        String storage,
        String ostype,
        @JsonProperty("network_bridge") String networkBridge
    ) {
    }

    private record ContainerListRequest(
        String node,
        @JsonProperty("include_stats") Boolean includeStats,
        @JsonProperty("include_raw") Boolean includeRaw,
        @JsonProperty("format_style") String formatStyle
    ) {
    }

    private record ContainerSelectorRequest(String selector, @JsonProperty("format_style") String formatStyle) {
    }

    private record StopContainerRequest(
        String selector,
        Boolean graceful,
        @JsonProperty("timeout_seconds") Integer timeoutSeconds,
        @JsonProperty("format_style") String formatStyle
    ) {
    }

    private record RestartContainerRequest(
        String selector,
        @JsonProperty("timeout_seconds") Integer timeoutSeconds,
        @JsonProperty("format_style") String formatStyle
    ) {
    }

    private record UpdateContainerRequest(
        String selector,
        Integer cores,
        Integer memory,
        Integer swap,
        @JsonProperty("disk_gb") Integer diskGb,
        String disk,
        @JsonProperty("format_style") String formatStyle
    ) {
    }

    private record CreateContainerRequest(
        String node,
        String vmid,
        String ostemplate,
        String hostname,
        Integer cores,
        Integer memory,
        Integer swap,
        @JsonProperty("disk_size") Integer diskSize,
        String storage,
        String password,
        @JsonProperty("ssh_public_keys") String sshPublicKeys,
        @JsonProperty("network_bridge") String networkBridge,
        @JsonProperty("start_after_create") Boolean startAfterCreate,
        Boolean unprivileged
    ) {
    }

    private record DeleteContainerRequest(
        String selector,
        Boolean force,
        @JsonProperty("format_style") String formatStyle
    ) {
    }

    private record SnapshotRequest(String node, String vmid, @JsonProperty("vm_type") String vmType) {
    }

    private record CreateSnapshotRequest(
        String node,
        String vmid,
        String snapname,
        String description,
        Boolean vmstate,
        @JsonProperty("vm_type") String vmType
    ) {
    }

    private record SnapshotDeleteRequest(
        String node,
        String vmid,
        String snapname,
        @JsonProperty("vm_type") String vmType
    ) {
    }

    private record IsoListRequest(String node, String storage) {
    }

    private record IsoDownloadRequest(
        String node,
        String storage,
        String url,
        String filename,
        String checksum,
        @JsonProperty("checksum_algorithm") String checksumAlgorithm
    ) {
    }

    private record IsoDeleteRequest(String node, String storage, String filename) {
    }

    private record BackupListRequest(String node, String storage, String vmid) {
    }

    private record CreateBackupRequest(
        String node,
        String vmid,
        String storage,
        String compress,
        String mode,
        String notes
    ) {
    }

    private record RestoreBackupRequest(
        String node,
        String archive,
        String vmid,
        String storage,
        Boolean unique
    ) {
    }

    private record DeleteBackupRequest(String node, String storage, String volid) {
    }
}
