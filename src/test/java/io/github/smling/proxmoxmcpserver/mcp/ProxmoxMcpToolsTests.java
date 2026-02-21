package io.github.smling.proxmoxmcpserver.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smling.proxmoxmcpserver.tools.BackupTools;
import io.github.smling.proxmoxmcpserver.tools.ClusterTools;
import io.github.smling.proxmoxmcpserver.tools.ContainerTools;
import io.github.smling.proxmoxmcpserver.tools.IsoTools;
import io.github.smling.proxmoxmcpserver.tools.NodeTools;
import io.github.smling.proxmoxmcpserver.tools.SnapshotTools;
import io.github.smling.proxmoxmcpserver.tools.StorageTools;
import io.github.smling.proxmoxmcpserver.tools.VmTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ProxmoxMcpToolsTests {
    private NodeTools nodeTools;
    private VmTools vmTools;
    private StorageTools storageTools;
    private ClusterTools clusterTools;
    private ContainerTools containerTools;
    private SnapshotTools snapshotTools;
    private IsoTools isoTools;
    private BackupTools backupTools;
    private ProxmoxMcpTools tools;

    @BeforeEach
    void setUp() {
        nodeTools = mock(NodeTools.class);
        vmTools = mock(VmTools.class);
        storageTools = mock(StorageTools.class);
        clusterTools = mock(ClusterTools.class);
        containerTools = mock(ContainerTools.class);
        snapshotTools = mock(SnapshotTools.class);
        isoTools = mock(IsoTools.class);
        backupTools = mock(BackupTools.class);
        tools = new ProxmoxMcpTools(
            nodeTools, vmTools, storageTools, clusterTools, containerTools, snapshotTools, isoTools, backupTools
        );
    }

    @Test
    void getNodesDelegates() {
        when(nodeTools.getNodes()).thenReturn("nodes");
        assertThat(tools.getNodes()).isEqualTo("nodes");
        verify(nodeTools).getNodes();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void getNodeStatusRequiresNode(String node) {
        assertThatThrownBy(() -> tools.getNodeStatus(node))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getNodeStatusDelegates() {
        when(nodeTools.getNodeStatus("pve1")).thenReturn("status");
        assertThat(tools.getNodeStatus("pve1")).isEqualTo("status");
        verify(nodeTools).getNodeStatus("pve1");
    }

    @Test
    void getVmsDelegates() {
        when(vmTools.getVms()).thenReturn("vms");
        assertThat(tools.getVms()).isEqualTo("vms");
        verify(vmTools).getVms();
    }

    @Test
    void createVmDelegates() {
        when(vmTools.createVm("pve1", "100", "vm", 2, 2048, 10, null, null, null)).thenReturn("created");
        assertThat(tools.createVm("pve1", "100", "vm", 2, 2048, 10, null, null, null))
            .isEqualTo("created");
        verify(vmTools).createVm("pve1", "100", "vm", 2, 2048, 10, null, null, null);
    }

    @Test
    void executeVmCommandDelegates() {
        when(vmTools.executeCommand("pve1", "100", "uptime")).thenReturn("out");
        assertThat(tools.executeVmCommand("pve1", "100", "uptime")).isEqualTo("out");
        verify(vmTools).executeCommand("pve1", "100", "uptime");
    }

    @Test
    void startVmDelegates() {
        when(vmTools.startVm("pve1", "100")).thenReturn("start");
        assertThat(tools.startVm("pve1", "100")).isEqualTo("start");
        verify(vmTools).startVm("pve1", "100");
    }

    @Test
    void stopVmDelegates() {
        when(vmTools.stopVm("pve1", "100")).thenReturn("stop");
        assertThat(tools.stopVm("pve1", "100")).isEqualTo("stop");
        verify(vmTools).stopVm("pve1", "100");
    }

    @Test
    void shutdownVmDelegates() {
        when(vmTools.shutdownVm("pve1", "100")).thenReturn("shutdown");
        assertThat(tools.shutdownVm("pve1", "100")).isEqualTo("shutdown");
        verify(vmTools).shutdownVm("pve1", "100");
    }

    @Test
    void resetVmDelegates() {
        when(vmTools.resetVm("pve1", "100")).thenReturn("reset");
        assertThat(tools.resetVm("pve1", "100")).isEqualTo("reset");
        verify(vmTools).resetVm("pve1", "100");
    }

    @Test
    void deleteVmDefaultsForceToFalse() {
        when(vmTools.deleteVm("pve1", "100", false)).thenReturn("deleted");
        assertThat(tools.deleteVm("pve1", "100", null)).isEqualTo("deleted");
        verify(vmTools).deleteVm("pve1", "100", false);
    }

    @Test
    void deleteVmHonorsForceTrue() {
        when(vmTools.deleteVm("pve1", "100", true)).thenReturn("deleted");
        assertThat(tools.deleteVm("pve1", "100", true)).isEqualTo("deleted");
        verify(vmTools).deleteVm("pve1", "100", true);
    }

    @Test
    void getStorageDelegates() {
        when(storageTools.getStorage()).thenReturn("storage");
        assertThat(tools.getStorage()).isEqualTo("storage");
        verify(storageTools).getStorage();
    }

    @Test
    void getClusterStatusDelegates() {
        when(clusterTools.getClusterStatus()).thenReturn("cluster");
        assertThat(tools.getClusterStatus()).isEqualTo("cluster");
        verify(clusterTools).getClusterStatus();
    }

    @Test
    void getContainersAppliesDefaults() {
        when(containerTools.getContainers(null, true, false, "pretty")).thenReturn("containers");
        assertThat(tools.getContainers(null, null, null, null)).isEqualTo("containers");
        verify(containerTools).getContainers(null, true, false, "pretty");
    }

    @Test
    void getContainersHonorsExplicitFlags() {
        when(containerTools.getContainers("pve1", false, true, "json")).thenReturn("containers");
        assertThat(tools.getContainers("pve1", false, true, "json")).isEqualTo("containers");
        verify(containerTools).getContainers("pve1", false, true, "json");
    }

    @Test
    void startContainerDefaultsFormatStyle() {
        when(containerTools.startContainer("101", "pretty")).thenReturn("started");
        assertThat(tools.startContainer("101", null)).isEqualTo("started");
        verify(containerTools).startContainer("101", "pretty");
    }

    @Test
    void startContainerHonorsFormatStyle() {
        when(containerTools.startContainer("101", "json")).thenReturn("started");
        assertThat(tools.startContainer("101", "json")).isEqualTo("started");
        verify(containerTools).startContainer("101", "json");
    }

    @Test
    void stopContainerDefaultsGracefulAndTimeout() {
        when(containerTools.stopContainer("101", true, 10, "pretty")).thenReturn("stopped");
        assertThat(tools.stopContainer("101", null, null, null)).isEqualTo("stopped");
        verify(containerTools).stopContainer("101", true, 10, "pretty");
    }

    @Test
    void stopContainerHonorsGracefulFalse() {
        when(containerTools.stopContainer("101", false, 5, "json")).thenReturn("stopped");
        assertThat(tools.stopContainer("101", false, 5, "json")).isEqualTo("stopped");
        verify(containerTools).stopContainer("101", false, 5, "json");
    }

    @Test
    void restartContainerDefaultsTimeout() {
        when(containerTools.restartContainer("101", 10, "pretty")).thenReturn("restarted");
        assertThat(tools.restartContainer("101", null, null)).isEqualTo("restarted");
        verify(containerTools).restartContainer("101", 10, "pretty");
    }

    @Test
    void restartContainerUsesProvidedTimeout() {
        when(containerTools.restartContainer("101", 30, "json")).thenReturn("restarted");
        assertThat(tools.restartContainer("101", 30, "json")).isEqualTo("restarted");
        verify(containerTools).restartContainer("101", 30, "json");
    }

    @Test
    void updateContainerResourcesDefaultsDisk() {
        when(containerTools.updateContainerResources("101", 2, 512, 0, 5, "rootfs", "pretty"))
            .thenReturn("updated");
        assertThat(tools.updateContainerResources("101", 2, 512, 0, 5, "", null)).isEqualTo("updated");
        verify(containerTools).updateContainerResources("101", 2, 512, 0, 5, "rootfs", "pretty");
    }

    @Test
    void updateContainerResourcesUsesProvidedDisk() {
        when(containerTools.updateContainerResources("101", 2, 512, 0, 5, "data", "json"))
            .thenReturn("updated");
        assertThat(tools.updateContainerResources("101", 2, 512, 0, 5, "data", "json")).isEqualTo("updated");
        verify(containerTools).updateContainerResources("101", 2, 512, 0, 5, "data", "json");
    }

    @Test
    void createContainerAppliesDefaults() {
        when(containerTools.createContainer("pve1", "101", "tmpl", null, 1, 512, 512, 8,
            null, null, null, "vmbr0", false, true)).thenReturn("created");

        assertThat(tools.createContainer("pve1", "101", "tmpl", null, null, null, null, null,
            null, null, null, null, null, null)).isEqualTo("created");

        verify(containerTools).createContainer("pve1", "101", "tmpl", null, 1, 512, 512, 8,
            null, null, null, "vmbr0", false, true);
    }

    @Test
    void createContainerUsesProvidedValues() {
        when(containerTools.createContainer("pve1", "101", "tmpl", "ct1", 4, 2048, 1024, 20,
            "fast", "pw", "ssh", "vmbr1", true, false)).thenReturn("created");

        assertThat(tools.createContainer("pve1", "101", "tmpl", "ct1", 4, 2048, 1024, 20,
            "fast", "pw", "ssh", "vmbr1", true, false)).isEqualTo("created");

        verify(containerTools).createContainer("pve1", "101", "tmpl", "ct1", 4, 2048, 1024, 20,
            "fast", "pw", "ssh", "vmbr1", true, false);
    }

    @Test
    void deleteContainerDefaultsForce() {
        when(containerTools.deleteContainer("101", false, "pretty")).thenReturn("deleted");
        assertThat(tools.deleteContainer("101", null, null)).isEqualTo("deleted");
        verify(containerTools).deleteContainer("101", false, "pretty");
    }

    @Test
    void listSnapshotsDefaultsVmType() {
        when(snapshotTools.listSnapshots("pve1", "101", "qemu")).thenReturn("snapshots");
        assertThat(tools.listSnapshots("pve1", "101", null)).isEqualTo("snapshots");
        verify(snapshotTools).listSnapshots("pve1", "101", "qemu");
    }

    @Test
    void listSnapshotsUsesProvidedVmType() {
        when(snapshotTools.listSnapshots("pve1", "101", "lxc")).thenReturn("snapshots");
        assertThat(tools.listSnapshots("pve1", "101", "lxc")).isEqualTo("snapshots");
        verify(snapshotTools).listSnapshots("pve1", "101", "lxc");
    }

    @Test
    void createSnapshotDefaultsVmStateAndType() {
        when(snapshotTools.createSnapshot("pve1", "101", "snap", null, false, "qemu")).thenReturn("created");
        assertThat(tools.createSnapshot("pve1", "101", "snap", null, null, null)).isEqualTo("created");
        verify(snapshotTools).createSnapshot("pve1", "101", "snap", null, false, "qemu");
    }

    @Test
    void createSnapshotHonorsVmStateAndType() {
        when(snapshotTools.createSnapshot("pve1", "101", "snap", "desc", true, "lxc")).thenReturn("created");
        assertThat(tools.createSnapshot("pve1", "101", "snap", "desc", true, "lxc")).isEqualTo("created");
        verify(snapshotTools).createSnapshot("pve1", "101", "snap", "desc", true, "lxc");
    }

    @Test
    void deleteSnapshotDefaultsVmType() {
        when(snapshotTools.deleteSnapshot("pve1", "101", "snap", "qemu")).thenReturn("deleted");
        assertThat(tools.deleteSnapshot("pve1", "101", "snap", null)).isEqualTo("deleted");
        verify(snapshotTools).deleteSnapshot("pve1", "101", "snap", "qemu");
    }

    @Test
    void rollbackSnapshotDefaultsVmType() {
        when(snapshotTools.rollbackSnapshot("pve1", "101", "snap", "qemu")).thenReturn("rolled");
        assertThat(tools.rollbackSnapshot("pve1", "101", "snap", null)).isEqualTo("rolled");
        verify(snapshotTools).rollbackSnapshot("pve1", "101", "snap", "qemu");
    }

    @Test
    void listIsosDelegates() {
        when(isoTools.listIsos("pve1", "local")).thenReturn("isos");
        assertThat(tools.listIsos("pve1", "local")).isEqualTo("isos");
        verify(isoTools).listIsos("pve1", "local");
    }

    @Test
    void listTemplatesDelegates() {
        when(isoTools.listTemplates("pve1", "local")).thenReturn("templates");
        assertThat(tools.listTemplates("pve1", "local")).isEqualTo("templates");
        verify(isoTools).listTemplates("pve1", "local");
    }

    @Test
    void downloadIsoDefaultsAlgorithm() {
        when(isoTools.downloadIso("pve1", "local", "http://example", "file.iso", null, "sha256"))
            .thenReturn("downloaded");
        assertThat(tools.downloadIso("pve1", "local", "http://example", "file.iso", null, null))
            .isEqualTo("downloaded");
        verify(isoTools).downloadIso("pve1", "local", "http://example", "file.iso", null, "sha256");
    }

    @Test
    void downloadIsoUsesProvidedAlgorithm() {
        when(isoTools.downloadIso("pve1", "local", "http://example", "file.iso", "sum", "sha512"))
            .thenReturn("downloaded");
        assertThat(tools.downloadIso("pve1", "local", "http://example", "file.iso", "sum", "sha512"))
            .isEqualTo("downloaded");
        verify(isoTools).downloadIso("pve1", "local", "http://example", "file.iso", "sum", "sha512");
    }

    @Test
    void deleteIsoDelegates() {
        when(isoTools.deleteIso("pve1", "local", "file.iso")).thenReturn("deleted");
        assertThat(tools.deleteIso("pve1", "local", "file.iso")).isEqualTo("deleted");
        verify(isoTools).deleteIso("pve1", "local", "file.iso");
    }

    @Test
    void listBackupsDelegates() {
        when(backupTools.listBackups("pve1", "backup", "101")).thenReturn("backups");
        assertThat(tools.listBackups("pve1", "backup", "101")).isEqualTo("backups");
        verify(backupTools).listBackups("pve1", "backup", "101");
    }

    @Test
    void createBackupDelegates() {
        when(backupTools.createBackup("pve1", "101", "backup", null, null, null)).thenReturn("created");
        assertThat(tools.createBackup("pve1", "101", "backup", null, null, null)).isEqualTo("created");
        verify(backupTools).createBackup("pve1", "101", "backup", null, null, null);
    }

    @Test
    void restoreBackupDefaultsUnique() {
        when(backupTools.restoreBackup("pve1", "archive", "202", null, true)).thenReturn("restored");
        assertThat(tools.restoreBackup("pve1", "archive", "202", null, null)).isEqualTo("restored");
        verify(backupTools).restoreBackup("pve1", "archive", "202", null, true);
    }

    @Test
    void restoreBackupHonorsUniqueFalse() {
        when(backupTools.restoreBackup("pve1", "archive", "202", null, false)).thenReturn("restored");
        assertThat(tools.restoreBackup("pve1", "archive", "202", null, false)).isEqualTo("restored");
        verify(backupTools).restoreBackup("pve1", "archive", "202", null, false);
    }

    @Test
    void deleteBackupDelegates() {
        when(backupTools.deleteBackup("pve1", "backup", "volid")).thenReturn("deleted");
        assertThat(tools.deleteBackup("pve1", "backup", "volid")).isEqualTo("deleted");
        verify(backupTools).deleteBackup("pve1", "backup", "volid");
    }
}
