package io.github.smling.proxmoxmcpserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.corsinvest.proxmoxve.api.Result;
import org.mockito.Mockito;

/**
 * Shared helpers for unit tests.
 */
public final class TestSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestSupport() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static Result resultWithData(JsonNode data) {
        ObjectNode response = MAPPER.createObjectNode();
        response.set("data", data);
        return resultWithResponse(response);
    }

    public static Result resultWithResponse(JsonNode response) {
        return Mockito.mock(Result.class, invocation -> {
            if ("getResponse".equals(invocation.getMethod().getName())) {
                return response;
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
    }
}
