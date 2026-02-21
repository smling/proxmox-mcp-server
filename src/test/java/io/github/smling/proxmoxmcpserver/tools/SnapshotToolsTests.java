package io.github.smling.proxmoxmcpserver.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.smling.proxmoxmcpserver.TestSupport;
import io.github.smling.proxmoxmcpserver.core.ProxmoxClient;
import org.junit.jupiter.api.Test;

class SnapshotToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void listSnapshotsReturnsEmptyMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        when(proxmox.get("/nodes/pve1/qemu/100/snapshot"))
            .thenReturn(TestSupport.resultWithData(mapper.createArrayNode()));

        String output = tools.listSnapshots("pve1", "100", "qemu");

        assertThat(output).contains("No snapshots found");
    }

    @Test
    void listSnapshotsRendersEntries() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        ArrayNode snaps = mapper.createArrayNode();
        snaps.add(mapper.createObjectNode().put("name", "current"));
        ObjectNode snap = mapper.createObjectNode();
        snap.put("name", "snap1");
        snap.put("description", "before");
        snaps.add(snap);

        when(proxmox.get("/nodes/pve1/qemu/100/snapshot"))
            .thenReturn(TestSupport.resultWithData(snaps));

        String output = tools.listSnapshots("pve1", "100", "qemu");

        assertThat(output).contains("snap1");
        assertThat(output).contains("Description: before");
    }

    @Test
    void listSnapshotsUsesLxcPath() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        when(proxmox.get("/nodes/pve1/lxc/100/snapshot"))
            .thenReturn(TestSupport.resultWithData(mapper.createArrayNode()));

        String output = tools.listSnapshots("pve1", "100", "lxc");

        assertThat(output).contains("No snapshots found for LXC 100");
    }

    @Test
    void listSnapshotsIncludesParentsAndVmState() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        ArrayNode snaps = mapper.createArrayNode();
        ObjectNode snap = mapper.createObjectNode();
        snap.put("name", "snap1");
        snap.put("parent", "base");
        snap.put("snaptime", 1000);
        snap.put("vmstate", true);
        snaps.add(snap);

        when(proxmox.get("/nodes/pve1/qemu/100/snapshot"))
            .thenReturn(TestSupport.resultWithData(snaps));

        String output = tools.listSnapshots("pve1", "100", "qemu");

        assertThat(output).contains("Parent: base");
        assertThat(output).contains("RAM State: Included");
    }

    @Test
    void createSnapshotReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/snapshot"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createSnapshot("pve1", "100", "snap1", "desc", true, "qemu");

        assertThat(output).contains("Snapshot Created Successfully");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void createSnapshotSkipsVmStateForLxc() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/lxc/100/snapshot"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createSnapshot("pve1", "100", "snap1", null, true, "lxc");

        assertThat(output).contains("LXC ID: 100");
        assertThat(output).doesNotContain("RAM State");
    }

    @Test
    void deleteSnapshotReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        when(proxmox.delete("/nodes/pve1/qemu/100/snapshot/snap1"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteSnapshot("pve1", "100", "snap1", "qemu");

        assertThat(output).contains("Snapshot Deleted");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void deleteSnapshotUsesLxcPath() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        when(proxmox.delete("/nodes/pve1/lxc/100/snapshot/snap1"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteSnapshot("pve1", "100", "snap1", "lxc");

        assertThat(output).contains("LXC ID: 100");
    }

    @Test
    void rollbackSnapshotDeletesChildrenAndReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        ArrayNode snaps = mapper.createArrayNode();
        snaps.add(mapper.createObjectNode().put("name", "current"));
        ObjectNode snap = mapper.createObjectNode();
        snap.put("name", "child");
        snap.put("parent", "snap1");
        snaps.add(snap);

        when(proxmox.get("/nodes/pve1/qemu/100/snapshot"))
            .thenReturn(TestSupport.resultWithData(snaps));
        when(proxmox.delete("/nodes/pve1/qemu/100/snapshot/child"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/snapshot/snap1/rollback"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.rollbackSnapshot("pve1", "100", "snap1", "qemu");

        assertThat(output).contains("Snapshot Rollback Initiated");
        assertThat(output).contains("Deleted newer snapshots: child");
    }

    @Test
    void rollbackSnapshotHandlesNonArraySnapshots() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);

        ObjectNode snaps = mapper.createObjectNode();
        when(proxmox.get("/nodes/pve1/qemu/100/snapshot"))
            .thenReturn(TestSupport.resultWithData(snaps));
        when(proxmox.postForm(eq("/nodes/pve1/qemu/100/snapshot/snap1/rollback"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.rollbackSnapshot("pve1", "100", "snap1", "qemu");

        assertThat(output).contains("Snapshot Rollback Initiated");
    }

    @Test
    void errorPayloadFallsBackWhenJsonFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        SnapshotTools tools = new SnapshotTools(proxmox);
        ObjectMapper original = SnapshotTools.swapObjectMapper(new ObjectMapper() {
            @Override
            public ObjectWriter writerWithDefaultPrettyPrinter() {
                throw new RuntimeException("boom");
            }
        });
        try {
            when(proxmox.get("/nodes/pve1/qemu/100/snapshot"))
                .thenThrow(new RuntimeException("boom"));

            String output = tools.listSnapshots("pve1", "100", "qemu");

            assertThat(output).contains("\"error\":\"boom\"");
        } finally {
            SnapshotTools.swapObjectMapper(original);
        }
    }

}
