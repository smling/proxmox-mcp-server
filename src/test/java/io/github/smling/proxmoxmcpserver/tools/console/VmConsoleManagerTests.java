package io.github.smling.proxmoxmcpserver.tools.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VmConsoleManagerTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void executeCommandReturnsConsoleOutput() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmConsoleManager manager = new VmConsoleManager(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current"))
            .thenReturn(TestSupport.resultWithData(status));

        ObjectNode exec = mapper.createObjectNode();
        exec.put("pid", "123");
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/agent/exec"), anyMap()))
            .thenReturn(TestSupport.resultWithData(exec));

        ObjectNode execStatus = mapper.createObjectNode();
        execStatus.put("out-data", "ok");
        execStatus.put("err-data", "");
        execStatus.put("exitcode", 0);
        when(proxmox.get(eq("/nodes/pve1/qemu/100/agent/exec-status"), anyMap()))
            .thenReturn(TestSupport.resultWithData(execStatus));

        Map<String, Object> result = manager.executeCommand("pve1", "100", "uptime");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("output")).isEqualTo("ok");
        assertThat(result.get("exit_code")).isEqualTo(0);
    }

    @Test
    void executeCommandRejectsStoppedVm() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmConsoleManager manager = new VmConsoleManager(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "stopped");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current"))
            .thenReturn(TestSupport.resultWithData(status));

        assertThatThrownBy(() -> manager.executeCommand("pve1", "100", "uptime"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not running");
    }

    @Test
    void executeCommandThrowsWhenPidMissing() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmConsoleManager manager = new VmConsoleManager(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current"))
            .thenReturn(TestSupport.resultWithData(status));

        ObjectNode exec = mapper.createObjectNode();
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/agent/exec"), anyMap()))
            .thenReturn(TestSupport.resultWithData(exec));

        assertThatThrownBy(() -> manager.executeCommand("pve1", "100", "uptime"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to execute command");
    }

    @Test
    void executeCommandHandlesNonObjectConsolePayload() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmConsoleManager manager = new VmConsoleManager(proxmox);

        ObjectNode status = mapper.createObjectNode();
        status.put("status", "running");
        when(proxmox.get("/nodes/pve1/qemu/100/status/current"))
            .thenReturn(TestSupport.resultWithData(status));

        ObjectNode exec = mapper.createObjectNode();
        exec.put("pid", "123");
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/agent/exec"), anyMap()))
            .thenReturn(TestSupport.resultWithData(exec));

        when(proxmox.get(eq("/nodes/pve1/qemu/100/agent/exec-status"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("ok")));

        Map<String, Object> result = manager.executeCommand("pve1", "100", "uptime");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("output")).isEqualTo("\"ok\"");
    }

    @Test
    void executeCommandMapsNotFoundErrors() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        VmConsoleManager manager = new VmConsoleManager(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/100/status/current"))
            .thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> manager.executeCommand("pve1", "100", "uptime"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }
}
