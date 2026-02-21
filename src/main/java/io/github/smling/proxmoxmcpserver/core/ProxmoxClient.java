package io.github.smling.proxmoxmcpserver.core;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import it.corsinvest.proxmoxve.api.PveClient;
import it.corsinvest.proxmoxve.api.Result;
import org.springframework.http.HttpMethod;

public class ProxmoxClient {
    private final PveClient client;
    private final URI baseUri;
    private final String apiToken;

    public ProxmoxClient(String host, int port, boolean verifySsl, String user, String tokenName, String tokenValue) {
        this.baseUri = URI.create("https://" + host + ":" + port + "/api2/json");
        this.apiToken = user + "!" + tokenName + "=" + tokenValue;
//        this.apiToken = tokenValue;
        this.client = new PveClient(host, port);
        this.client.setValidateCertificate(verifySsl);
        this.client.setApiToken(this.apiToken);
        this.client.setTimeout(10_000);
    }

    public void testConnection() throws Exception {
        Result result = client.getVersion().version();
        if (result == null || !result.isSuccessStatusCode()) {
            String reason = result == null ? "No response" : result.getReasonPhrase();
            throw new IllegalStateException("Proxmox API error: " + reason);
        }
    }

    public Result get(String path) throws Exception {
        return get(path, Map.of());
    }

    public Result get(String path, Map<String, String> query) throws Exception {
        return exchange(path, HttpMethod.GET, query, null);
    }

    public Result postForm(String path, Map<String, String> form) throws Exception {
        return exchange(path, HttpMethod.POST, Map.of(), form);
    }

    public Result putForm(String path, Map<String, String> form) throws Exception {
        return exchange(path, HttpMethod.PUT, Map.of(), form);
    }

    public Result delete(String path) throws Exception {
        return exchange(path, HttpMethod.DELETE, Map.of(), null);
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public String getAuthHeaderValue() {
        return "PVEAPIToken " + apiToken;
    }

    private Result exchange(String path, HttpMethod method, Map<String, String> query,
                            Map<String, String> form) throws Exception {
        Result result;
        if (HttpMethod.GET.equals(method)) {
            result = client.get(path, toObjectMap(query));
        } else if (HttpMethod.POST.equals(method)) {
            result = client.create(path, toObjectMap(form));
        } else if (HttpMethod.PUT.equals(method)) {
            result = client.set(path, toObjectMap(form));
        } else if (HttpMethod.DELETE.equals(method)) {
            result = client.delete(path, null);
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        if (result == null) {
            throw new IllegalStateException("Proxmox API error: no response");
        }
        if (!result.isSuccessStatusCode()) {
            throw new IllegalStateException("Proxmox API error: " + result.getStatusCode() + " " + result.getReasonPhrase());
        }
        if (result.responseInError()) {
            throw new IllegalStateException("Proxmox API error: " + result.getError());
        }

        return result;
    }

    private static Map<String, Object> toObjectMap(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        data.forEach((key, value) -> {
            if (value != null) {
                params.put(key, value);
            }
        });
        return params.isEmpty() ? null : params;
    }
}
