package io.github.smling.proxmoxmcpserver.core;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import it.corsinvest.proxmoxve.api.PveClient;
import it.corsinvest.proxmoxve.api.Result;
import org.springframework.http.HttpMethod;

/**
 * Wraps the Proxmox API client with convenience helpers and validation.
 */
public class ProxmoxClient {
    private final PveClient client;
    private final URI baseUri;
    private final String apiToken;

    /**
     * Creates a Proxmox API client configured for token authentication.
     *
     * @param host the Proxmox host
     * @param port the Proxmox API port
     * @param verifySsl whether to validate TLS certificates
     * @param user the API user
     * @param tokenName the token name
     * @param tokenValue the token value
     */
    public ProxmoxClient(String host, int port, boolean verifySsl, String user, String tokenName, String tokenValue) {
        this.baseUri = URI.create("https://" + host + ":" + port + "/api2/json");
        this.apiToken = user + "!" + tokenName + "=" + tokenValue;
//        this.apiToken = tokenValue;
        this.client = new PveClient(host, port);
        this.client.setValidateCertificate(verifySsl);
        this.client.setApiToken(this.apiToken);
        this.client.setTimeout(10_000);
    }

    /**
     * Performs a lightweight API call to verify connectivity and credentials.
     *
     * @throws Exception when the API is unreachable or returns errors
     */
    public void testConnection() throws Exception {
        Result result = client.getVersion().version();
        if (result == null || !result.isSuccessStatusCode()) {
            String reason = result == null ? "No response" : result.getReasonPhrase();
            throw new IllegalStateException("Proxmox API error: " + reason);
        }
    }

    /**
     * Issues a GET request to the Proxmox API.
     *
     * @param path the API path
     * @return the API result
     * @throws Exception when the request fails
     */
    public Result get(String path) throws Exception {
        return get(path, Map.of());
    }

    /**
     * Issues a GET request with query parameters.
     *
     * @param path the API path
     * @param query query parameters
     * @return the API result
     * @throws Exception when the request fails
     */
    public Result get(String path, Map<String, String> query) throws Exception {
        return exchange(path, HttpMethod.GET, query, null);
    }

    /**
     * Issues a POST request with form parameters.
     *
     * @param path the API path
     * @param form form parameters
     * @return the API result
     * @throws Exception when the request fails
     */
    public Result postForm(String path, Map<String, String> form) throws Exception {
        return exchange(path, HttpMethod.POST, Map.of(), form);
    }

    /**
     * Issues a PUT request with form parameters.
     *
     * @param path the API path
     * @param form form parameters
     * @return the API result
     * @throws Exception when the request fails
     */
    public Result putForm(String path, Map<String, String> form) throws Exception {
        return exchange(path, HttpMethod.PUT, Map.of(), form);
    }

    /**
     * Issues a DELETE request.
     *
     * @param path the API path
     * @return the API result
     * @throws Exception when the request fails
     */
    public Result delete(String path) throws Exception {
        return exchange(path, HttpMethod.DELETE, Map.of(), null);
    }

    /**
     * Returns the base URI for the Proxmox API.
     *
     * @return the API base URI
     */
    public URI getBaseUri() {
        return baseUri;
    }

    /**
     * Returns the value for the Proxmox API token auth header.
     *
     * @return the authorization header value
     */
    public String getAuthHeaderValue() {
        return "PVEAPIToken " + apiToken;
    }

    /**
     * Executes an HTTP request and validates the response.
     *
     * @param path the API path
     * @param method the HTTP method
     * @param query query parameters
     * @param form form parameters
     * @return the API result
     * @throws Exception when the API returns errors
     */
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

    /**
     * Converts a string map to an object map without null values.
     *
     * @param data the string map
     * @return the object map or {@code null} when empty
     */
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
