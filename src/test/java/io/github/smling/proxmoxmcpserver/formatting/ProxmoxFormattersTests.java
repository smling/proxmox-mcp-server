package io.github.smling.proxmoxmcpserver.formatting;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

class ProxmoxFormattersTests {

    @ParameterizedTest
    @CsvSource({
        "0,0.00 B",
        "1024,1.00 KB",
        "1048576,1.00 MB",
        "1073741824,1.00 GB"
    })
    void formatBytesUsesExpectedUnits(long bytes, String expected) {
        assertThat(ProxmoxFormatters.formatBytes(bytes)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "0,0m",
        "60,1m",
        "3600,1h",
        "86400,1d",
        "90061,1d 1h 1m"
    })
    void formatUptimeBuildsReadableString(long seconds, String expected) {
        assertThat(ProxmoxFormatters.formatUptime(seconds)).isEqualTo(expected);
    }

    @Test
    void formatCommandOutputIncludesErrorWhenPresent() {
        String output = ProxmoxFormatters.formatCommandOutput(false, "ls", "out", "err");
        assertThat(output).contains("FAILED");
        assertThat(output).contains("Command: ls");
        assertThat(output).contains("Output:");
        assertThat(output).contains("Error:");
    }

    @Test
    void formatCommandOutputSkipsErrorWhenBlank() {
        String output = ProxmoxFormatters.formatCommandOutput(true, "pwd", "here", " ");
        assertThat(output).contains("SUCCESS");
        assertThat(output).contains("Command: pwd");
        assertThat(output).doesNotContain("Error:");
    }

    @ParameterizedTest
    @NullSource
    void formatCommandOutputHandlesNullOutput(String output) {
        String result = ProxmoxFormatters.formatCommandOutput(true, "whoami", output, "");
        assertThat(result).contains("Output:");
    }

    @Test
    void constructorIsPrivate() throws Exception {
        Constructor<ProxmoxFormatters> ctor = ProxmoxFormatters.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);
        assertThat(ctor.newInstance()).isNotNull();
    }
}
