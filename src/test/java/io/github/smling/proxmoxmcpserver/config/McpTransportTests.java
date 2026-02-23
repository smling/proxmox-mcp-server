package io.github.smling.proxmoxmcpserver.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class doMcpTransportTests {

    @ParameterizedTest
    @NullAndEmptySource
    void fromStringDefaultsToStdioWhenBlank(String value) {
        assertThat(McpTransport.fromString(value)).isEqualTo(McpTransport.STDIO);
    }

    @ParameterizedTest
    @CsvSource({
        "stdio,STDIO",
        "sse,SSE",
        "streamable,STREAMABLE",
        "streamable_http,STREAMABLE"
    })
    void fromStringParsesKnownValues(String value, McpTransport expected) {
        assertThat(McpTransport.fromString(value)).isEqualTo(expected);
    }

    @Test
    void fromStringThrowsForUnknownValue() {
        assertThatThrownBy(() -> McpTransport.fromString("bad"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
