package com.github.spurreiter.keycloak.mfa.rest;

import org.keycloak.authentication.AuthenticationFlowContext;
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
import com.github.spurreiter.keycloak.mfa.util.MfaHelper;

public class MfaRequest {
    private static final Logger logger = Logger.getLogger(MfaRequest.class);

    public static final String REST_ENDPOINT = "restEndpoint";
    public static final String REST_ENDPOINT_USER = "restEndpointUser";
    public static final String REST_ENDPOINT_PWD = "restEndpointPwd";

    private String url;

    private String authHeader;

    private String xRequestId;

    public MfaRequest() {
    }

    public MfaRequest(String url) {
        this.url = url;
    }

    private static String setValue (String str, String def) {
        if (str == null || str.isBlank()) {
            return def; 
        } else {
            return str;
        }
    }

    public static MfaRequest buildRequest(AuthenticationFlowContext context) {
        Map<String, String> config = MfaHelper.getConfig(context);
        String url = config.get(REST_ENDPOINT);
        if (url == null || url.isBlank()) {
            url = "/";
        }
        if (url.charAt(0) == '/') {
            url = System.getenv("MFA_URL") + url;
        }
        String basicUser = setValue(config.get(REST_ENDPOINT_USER), System.getenv("MFA_USERNAME"));
        String basicPass = setValue(config.get(REST_ENDPOINT_PWD), System.getenv("MFA_PASSWORD"));
        return new MfaRequest(url).setBasicAuth(basicUser, basicPass).setRequestId(MfaHelper.getRequestId(context));
    }

    public MfaResponse send(Map<String, List<String>> userAttributes) {
        HashMap<String, Object> map = new HashMap<>();
        return request("POST", "/", "sendOtp", userAttributes, map);
    }

    public MfaResponse verify(Map<String, List<String>> userAttributes, String code) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", code);
        return request("PUT", "/", "verifyOtp", userAttributes, map);
    }

    public MfaResponse sendVerifyEmail(Map<String, List<String>> userAttributes, String link,
            long expirationInMinutes) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("link", link);
        map.put("expirationInMinutes", expirationInMinutes);
        return request("POST", "/", "sendVerifyEmail", userAttributes, map);
    }

    public MfaResponse sendResetEmail(Map<String, List<String>> userAttributes, String link,
            long expirationInMinutes) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("link", link);
        map.put("expirationInMinutes", expirationInMinutes);
        return request("POST", "/", "sendResetEmail", userAttributes, map);
    }

    public MfaResponse request(String method, String pathname, String useCase, Map<String, List<String>> userAttributes, 
            HashMap<String, Object> map) {
        String nonce = UUID.randomUUID().toString();
        map.put("useCase", useCase);
        map.put("nonce", nonce);

        String json = jsonBuilder(userAttributes, map);
        Response response = request(method, pathname, Entity.entity(json, MediaType.APPLICATION_JSON));
        logResponse(response, useCase);

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
            logger.error(e);
        }
        return "null";
    }

    private static void logResponse(Response response, String state) {
        int status = 500;
        if (response != null) {
            status = response.getStatus();
        }
        if (status >= 500) {
            logger.errorf("request failed. state=%s status=%s", state, status);
        } else if (status >= 400) {
            logger.warnf("request failed. state=%s status=%s", state, status);
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
