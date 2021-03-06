package com.github.spurreiter.keycloak.mfa.rest;

import org.keycloak.common.util.Base64;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MfaRequest {
    private static final Logger log = Logger.getLogger(MfaRequest.class);

    private String url;

    private String authHeader;

    private String xRequestId;

    public MfaRequest() {
    }

    public MfaRequest(String url) {
        this.url = url;
    }

    public MfaResponse send(Map<String, List<String>> userAttributes) {
        String nonce = UUID.randomUUID().toString();
        HashMap<String, Object> map = new HashMap<>();
        map.put("nonce", nonce);

        String json = jsonBuilder(userAttributes, map);
        Response response = request("POST", "/", Entity.entity(json, MediaType.APPLICATION_JSON));
        logResponse(response, "send");

        MfaResponse mfaRes = new MfaResponse(response);
        mfaRes.verifyNonce(nonce);
        return mfaRes;
    }

    public MfaResponse verify(Map<String, List<String>> userAttributes, String code) {
        String nonce = UUID.randomUUID().toString();
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("nonce", nonce);

        String json = jsonBuilder(userAttributes, map);
        Response response = request("PUT", "/verify", Entity.entity(json, MediaType.APPLICATION_JSON));
        logResponse(response, "verify");

        MfaResponse mfaRes = new MfaResponse(response);
        mfaRes.verifyNonce(nonce);
        return mfaRes;
    }

    public MfaRequest setBasicAuth(String basicAuthUsername, String basicAuthPassword) {
        if (basicAuthUsername != null && basicAuthPassword != null && !basicAuthUsername.isEmpty()
                && !basicAuthPassword.isEmpty()) {
            String auth = String.join("", basicAuthUsername, ":", basicAuthPassword);
            byte[] encodedAuth = Base64.encodeBytesToBytes(auth.getBytes(StandardCharsets.ISO_8859_1));
            this.authHeader = "Basic " + new String(encodedAuth);
        }
        return this;
    }

    public MfaRequest setRequestId(String xRequestId) {
        if (xRequestId != null && !xRequestId.isEmpty()) {
            this.xRequestId = xRequestId;
        }
        return this;
    }

    private MultivaluedMap<String, Object> getHeaders() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        if (this.authHeader != null) {
            headers.add(HttpHeaders.AUTHORIZATION, this.authHeader);
        }
        if (this.xRequestId != null) {
            headers.add("X-Request-ID", this.xRequestId);
        }
        return headers;
    }

    // private static String jsonBuilder(Map<String, List<String>> userAttributes) {
    //     HashMap<String, Object> map = new HashMap<>();
    //     return jsonBuilder(userAttributes, map);
    // }

    private static String jsonBuilder(Map<String, List<String>> userAttributes, HashMap<String, Object> map) {
        userAttributes.entrySet().stream().forEach(e -> {
            String key = e.getKey();
            if (map.get(key) == null) {
                map.put(key, e.getValue().get(0));
            }
        });
        try {
            String json = new ObjectMapper().writeValueAsString(map);
            return json;
        } catch (JsonProcessingException e) {
            log.error(e);
        }
        return "null";
    }

    private static void logResponse(Response response, String state) {
        int status = 500;
        if (response != null) {
            status = response.getStatus();
        }
        if (status >= 500) {
            log.errorf("request failed. state=%s status=%s", state, status);
        } else if (status >= 400) {
            log.warnf("request failed. state=%s status=%s", state, status);
        }
    }

    protected Response request(String method, String path, Entity<?> entity) {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
        clientBuilder.readTimeout(5, TimeUnit.SECONDS);

        Client client = clientBuilder.build();
        try {
            return client.target(this.url + path).request(MediaType.APPLICATION_JSON).headers(getHeaders())
                    .method(method, entity);
        } catch (Exception e) {
            return null;
        }
    }
}
