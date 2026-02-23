package io.github.smling.proxmoxmcpserver.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import io.github.smling.proxmoxmcpserver.tools.console.VmConsoleManager;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class VmToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void getVmsBuildsFormattedOutput() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        ObjectNode node = mapper.createObjectNode();
        node.put("node", "pve1");
        nodes.add(node);

        ArrayNode vms = mapper.createArrayNode();
        ObjectNode vm = mapper.createObjectNode();
        vm.put("vmid", "100");
        vm.put("name", "vm1");
        vm.put("status", "running");
        vm.put("mem", 1);
        vm.put("maxmem", 2);
        vms.add(vm);

        ObjectNode config = mapper.createObjectNode();
        config.put("cores", 2);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/qemu")).thenReturn(TestSupport.resultWithData(vms));
        when(proxmox.get("/nodes/pve1/qemu/100/config")).thenReturn(TestSupport.resultWithData(config));

        String output = tools.getVms();

        assertThat(output).contains("Virtual Machines");
        assertThat(output).contains("VM: vm1");
    }

    @Test
    void getVmsSkipsBadNodesAndHandlesConfigErrors() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode());
        nodes.add(mapper.createObjectNode().put("node", "pve1"));
        nodes.add(mapper.createObjectNode().put("node", "pve2"));

        ArrayNode vms = mapper.createArrayNode();
        ObjectNode vm = mapper.createObjectNode();
        vm.put("vmid", "200");
        vm.put("name", "vm2");
        vm.put("status", "stopped");
        vms.add(vm);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/qemu")).thenThrow(new RuntimeException("boom"));
        when(proxmox.get("/nodes/pve2/qemu")).thenReturn(TestSupport.resultWithData(vms));
        when(proxmox.get("/nodes/pve2/qemu/200/config")).thenThrow(new RuntimeException("boom"));

        String output = tools.getVms();

        assertThat(output).contains("VM: vm2");
        assertThat(output).contains("CPU Cores: N/A");
    }

    @Test
    void createVmThrowsWhenAlreadyExists() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/100/config"))
            .thenReturn(TestSupport.resultWithData(mapper.createObjectNode()));

        assertThatThrownBy(() -> tools.createVm("pve1", "100", "vm1", 1, 512, 10,
            null, null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createVmBuildsCreationSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/100/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "local-lvm");
        store.put("content", "images");
        store.put("type", "lvmthin");
        storage.add(store);

        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));
        when(proxmox.postForm(eq("/nodes/pve1/qemu"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createVm("pve1", "100", "vm1", 2, 2048, 10, null, null, null);

        assertThat(output).contains("VM 100 created successfully");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void createVmSelectsLocalLvmStorageAndAddsCloudInitNote() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/100/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "local-lvm");
        store.put("content", "images");
        store.put("type", "lvmthin");
        storage.add(store);
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));

        when(proxmox.postForm(eq("/nodes/pve1/qemu"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createVm("pve1", "100", "vm1", 2, 2048, 10, null, null, null);

        assertThat(output).contains("Storage Type: lvmthin");
        assertThat(output).contains("LVM storage does not support cloud-init image");
    }

    @Test
    void createVmSelectsVmStorageWhenPreferredMissing() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/101/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "vm-storage");
        store.put("content", "images");
        store.put("type", "dir");
        storage.add(store);
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));

        when(proxmox.postForm(eq("/nodes/pve1/qemu"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createVm("pve1", "101", "vm2", 2, 1024, 5, null, "l26", "vmbr1");

        assertThat(output).contains("Disk: 5 GB (vm-storage, qcow2 format)");
        assertThat(output).contains("Network: virtio (bridge=vmbr1)");
    }

    @Test
    void createVmSelectsFirstImageStorageWhenNoPreferred() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/102/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "fast");
        store.put("content", "images");
        store.put("type", "rbd");
        storage.add(store);
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));

        when(proxmox.postForm(eq("/nodes/pve1/qemu"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createVm("pve1", "102", "vm3", 1, 512, 4, null, "l26", "vmbr0");

        assertThat(output).contains("Disk: 4 GB (fast, raw format)");
    }

    @Test
    void createVmRejectsMissingStorage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/103/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        storage.add(mapper.createObjectNode().put("storage", "iso").put("content", "iso"));
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));

        assertThatThrownBy(() -> tools.createVm("pve1", "103", "vm4", 1, 512, 4, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No suitable storage");
    }

    @Test
    void createVmRejectsStorageNotFound() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/104/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        storage.add(mapper.createObjectNode().put("storage", "local").put("content", "images").put("type", "dir"));
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));

        assertThatThrownBy(() -> tools.createVm("pve1", "104", "vm5", 1, 512, 4, "missing", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void createVmRejectsStorageWithoutImages() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/105/config"))
            .thenThrow(new RuntimeException("not found"));

        ArrayNode storage = mapper.createArrayNode();
        storage.add(mapper.createObjectNode().put("storage", "iso-store").put("content", "iso").put("type", "dir"));
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));

        assertThatThrownBy(() -> tools.createVm("pve1", "105", "vm6", 1, 512, 4, "iso-store", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not support VM images");
    }

    @Test
    void createVmMapsPermissionDeniedDuringExistCheck() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/106/config"))
            .thenThrow(new RuntimeException("permission denied"));

        assertThatThrownBy(() -> tools.createVm("pve1", "106", "vm7", 1, 512, 4, "local", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Permission denied");
    }

    @Test
    void startVmReturnsAlreadyRunning() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));

        assertThat(tools.startVm("pve1", "100")).contains("already running");
    }

    @Test
    void startVmInitiatesWhenStopped() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/status/start"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        assertThat(tools.startVm("pve1", "100")).contains("start initiated");
    }

    @Test
    void stopVmReturnsAlreadyStopped() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));

        assertThat(tools.stopVm("pve1", "100")).contains("already stopped");
    }

    @Test
    void stopVmInitiatesWhenRunning() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/status/stop"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        assertThat(tools.stopVm("pve1", "100")).contains("stop initiated");
    }

    @Test
    void shutdownVmReturnsAlreadyStopped() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));

        assertThat(tools.shutdownVm("pve1", "100")).contains("already stopped");
    }

    @Test
    void shutdownVmInitiatesWhenRunning() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/status/shutdown"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        assertThat(tools.shutdownVm("pve1", "100")).contains("graceful shutdown initiated");
    }

    @Test
    void resetVmRejectsStoppedVm() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));

        assertThat(tools.resetVm("pve1", "100")).contains("Cannot reset VM");
    }

    @Test
    void resetVmInitiatesWhenRunning() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/status/reset"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        assertThat(tools.resetVm("pve1", "100")).contains("reset initiated");
    }

    @Test
    void executeCommandFormatsOutput() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);
        VmConsoleManager consoleManager = mock(VmConsoleManager.class);
        setField(tools, "consoleManager", consoleManager);

        when(consoleManager.executeCommand("pve1", "100", "uptime"))
            .thenReturn(Map.of("success", true, "output", "ok", "error", "", "exit_code", 0));

        String output = tools.executeCommand("pve1", "100", "uptime");

        assertThat(output).contains("Console Command Result");
        assertThat(output).contains("SUCCESS");
    }

    @Test
    void executeCommandPropagatesError() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);
        VmConsoleManager consoleManager = mock(VmConsoleManager.class);
        setField(tools, "consoleManager", consoleManager);
        doThrow(new RuntimeException("boom")).when(consoleManager).executeCommand("pve1", "100", "uptime");

        assertThatThrownBy(() -> tools.executeCommand("pve1", "100", "uptime"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteVmRejectsRunningWithoutForce() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        status.put("name", "vm1");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));

        assertThatThrownBy(() -> tools.deleteVm("pve1", "100", false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteVmStopsWhenForceEnabled() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        status.put("name", "vm1");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/status/stop"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));
        when(proxmox.delete("/nodes/pve1/qemu/100"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteVm("pve1", "100", true);

        assertThat(output).contains("Stopping VM");
        assertThat(output).contains("deletion initiated");
    }

    @Test
    void deleteVmDeletesWhenStopped() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        status.put("name", "vm1");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current")).thenReturn(TestSupport.resultWithData(status));
        when(proxmox.delete("/nodes/pve1/qemu/100"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteVm("pve1", "100", false);

        assertThat(output).contains("Deleting VM 100");
        assertThat(output).contains("Task ID: TASK");
    }

    @ParameterizedTest
    @MethodSource("notFoundOperations")
    void vmOperationsThrowWhenNotFound(VmOperation operation) throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get(anyString())).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> operation.apply(tools))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    void startVmHandlesNullMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmTools tools = new VmTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/100/status/current"))
            .thenThrow(new RuntimeException((String) null));

        assertThatThrownBy(() -> tools.startVm("pve1", "100"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to start VM");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static java.util.stream.Stream<VmOperation> notFoundOperations() {
        return java.util.stream.Stream.of(
            tools -> tools.startVm("pve1", "100"),
            tools -> tools.stopVm("pve1", "100"),
            tools -> tools.shutdownVm("pve1", "100"),
            tools -> tools.resetVm("pve1", "100"),
            tools -> tools.deleteVm("pve1", "100", false)
        );
    }

    @FunctionalInterface
    private interface VmOperation {
        String apply(VmTools tools) throws Exception;
    }
}
