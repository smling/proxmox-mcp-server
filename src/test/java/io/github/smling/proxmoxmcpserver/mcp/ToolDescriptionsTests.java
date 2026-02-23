package io.github.smling.proxmoxmcpserver.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class ToolDescriptionsTests {

    @ParameterizedTest
    @CsvSource({
        "GET_NODES_DESC",
        "GET_NODE_STATUS_DESC",
        "GET_VMS_DESC",
        "CREATE_VM_DESC",
        "EXECUTE_VM_COMMAND_DESC",
        "GET_STORAGE_DESC",
        "GET_CLUSTER_STATUS_DESC"
    })
    void toolDescriptionsArePopulated(String fieldName) throws Exception {
        String value = (String) ToolDescriptions.class.getField(fieldName).get(null);
        assertThat(value).isNotBlank();
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<ToolDescriptions> ctor = ToolDescriptions.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}
