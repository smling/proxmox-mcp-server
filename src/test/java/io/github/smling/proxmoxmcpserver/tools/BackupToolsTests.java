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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BackupToolsTests {

    private final ObjectMapper mapper = TestSupport.mapper();

    @Test
    void listBackupsReturnsEmptyMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        seedBackupContent(proxmox, mapper.createArrayNode());

        String output = tools.listBackups(null, null, null);

        assertThat(output).contains("No backups found");
    }

    @Test
    void listBackupsIncludesFiltersInEmptyMessage() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        seedBackupContent(proxmox, mapper.createArrayNode());

        String output = tools.listBackups("pve1", "backup", "100");

        assertThat(output).contains("No backups found on node pve1");
        assertThat(output).contains("in storage backup");
        assertThat(output).contains("for VM/CT 100");
    }

    @Test
    void listBackupsRendersEntries() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        ObjectNode item = mapper.createObjectNode();
        item.put("volid", "backup:backup/vzdump-qemu-100.vma.zst");
        item.put("vmid", "100");
        item.put("size", 2048);
        item.put("ctime", 1000);
        item.put("format", "vma");
        content.add(item);

        seedBackupContent(proxmox, content);

        String output = tools.listBackups(null, null, null);

        assertThat(output).contains("Available Backups");
        assertThat(output).contains("Volume ID: backup:backup/vzdump-qemu-100.vma.zst");
    }

    @Test
    void listBackupsSkipsNonBackupStorages() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode());
        nodes.add(mapper.createObjectNode().put("node", "pve1"));

        ArrayNode storage = mapper.createArrayNode();
        storage.add(mapper.createObjectNode());
        storage.add(mapper.createObjectNode().put("storage", "local").put("content", "images"));
        storage.add(mapper.createObjectNode().put("storage", "backup").put("content", "backup"));

        ArrayNode content = mapper.createArrayNode();
        ObjectNode item = mapper.createObjectNode();
        item.put("volid", "backup:backup/vzdump-qemu-100.vma.zst");
        item.put("vmid", "100");
        item.put("notes", "nightly");
        item.put("protected", true);
        item.put("ctime", 0);
        content.add(item);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));
        when(proxmox.get(eq("/nodes/pve1/storage/backup/content"), anyMap()))
            .thenReturn(TestSupport.resultWithData(content));

        String output = tools.listBackups(null, null, null);

        assertThat(output).contains("Notes: nightly");
        assertThat(output).contains("Protected");
        assertThat(output).contains("Unknown");
    }

    @Test
    void createBackupReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/vzdump"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createBackup("pve1", "100", "backup", null, null, "notes");

        assertThat(output).contains("Backup Started");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void createBackupHonorsCompressionOverrides() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/vzdump"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.createBackup("pve1", "100", "backup", "gzip", "stop", null);

        assertThat(output).contains("Compression: gzip");
        assertThat(output).contains("Mode: stop");
    }

    @ParameterizedTest
    @CsvSource({
        "backup:backup/vzdump-lxc-100.tar.zst,Container Restore Started",
        "backup:backup/vzdump-qemu-100.vma.zst,VM Restore Started"
    })
    void restoreBackupHandlesVmTypes(String archive, String expected) throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        String path = archive.contains("lxc") ? "/nodes/pve1/lxc" : "/nodes/pve1/qemu";
        when(proxmox.postForm(eq(path), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.restoreBackup("pve1", archive, "200", null, true);

        assertThat(output).contains(expected);
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void restoreBackupIncludesStorageAndUniqueNo() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        when(proxmox.postForm(eq("/nodes/pve1/qemu"), anyMap()))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.restoreBackup("pve1", "backup:backup/vzdump-qemu-100.vma.zst", "200", "backup", false);

        assertThat(output).contains("Target Storage: backup");
        assertThat(output).contains("Unique MACs: No");
    }

    @Test
    void deleteBackupRejectsProtected() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        ObjectNode item = mapper.createObjectNode();
        item.put("volid", "backup:backup/vzdump-qemu-100.vma.zst");
        item.put("protected", true);
        content.add(item);

        when(proxmox.get(eq("/nodes/pve1/storage/backup/content"), anyMap()))
            .thenReturn(TestSupport.resultWithData(content));

        String output = tools.deleteBackup("pve1", "backup",
            "backup:backup/vzdump-qemu-100.vma.zst");

        assertThat(output).contains("protected");
    }

    @Test
    void deleteBackupReturnsSummary() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        ArrayNode content = mapper.createArrayNode();
        ObjectNode item = mapper.createObjectNode();
        item.put("volid", "backup:backup/vzdump-qemu-100.vma.zst");
        item.put("protected", false);
        content.add(item);

        when(proxmox.get(eq("/nodes/pve1/storage/backup/content"), anyMap()))
            .thenReturn(TestSupport.resultWithData(content));
        when(proxmox.delete("/nodes/pve1/storage/backup/content/backup:backup/vzdump-qemu-100.vma.zst"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteBackup("pve1", "backup",
            "backup:backup/vzdump-qemu-100.vma.zst");

        assertThat(output).contains("Backup Deleted");
        assertThat(output).contains("Task ID:");
        assertThat(output).contains("TASK");
    }

    @Test
    void deleteBackupProceedsWhenInfoMissing() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);

        ArrayNode content = mapper.createArrayNode();

        when(proxmox.get(eq("/nodes/pve1/storage/backup/content"), anyMap()))
            .thenReturn(TestSupport.resultWithData(content));
        when(proxmox.delete("/nodes/pve1/storage/backup/content/backup:backup/vzdump-qemu-200.vma.zst"))
            .thenReturn(TestSupport.resultWithData(mapper.getNodeFactory().textNode("TASK")));

        String output = tools.deleteBackup("pve1", "backup",
            "backup:backup/vzdump-qemu-200.vma.zst");

        assertThat(output).contains("Backup Deleted");
    }

    @Test
    void errorPayloadFallsBackWhenJsonFails() throws Exception {
        ProxmoxClient proxmox = mock(ProxmoxClient.class);
        BackupTools tools = new BackupTools(proxmox);
        ObjectMapper original = BackupTools.swapObjectMapper(new ObjectMapper() {
            @Override
            public ObjectWriter writerWithDefaultPrettyPrinter() {
                throw new RuntimeException("boom");
            }
        });
        try {
            when(proxmox.get("/nodes")).thenThrow(new RuntimeException("boom"));

            String output = tools.listBackups(null, null, null);

            assertThat(output).contains("\"error\":\"boom\"");
        } finally {
            BackupTools.swapObjectMapper(original);
        }
    }

    private void seedBackupContent(ProxmoxClient proxmox, ArrayNode content) throws Exception {
        ArrayNode nodes = mapper.createArrayNode();
        nodes.add(mapper.createObjectNode().put("node", "pve1"));

        ArrayNode storage = mapper.createArrayNode();
        ObjectNode store = mapper.createObjectNode();
        store.put("storage", "backup");
        store.put("content", "backup");
        storage.add(store);

        when(proxmox.get("/nodes")).thenReturn(TestSupport.resultWithData(nodes));
        when(proxmox.get("/nodes/pve1/storage")).thenReturn(TestSupport.resultWithData(storage));
        when(proxmox.get(eq("/nodes/pve1/storage/backup/content"), anyMap()))
            .thenReturn(TestSupport.resultWithData(content));
    }

}
