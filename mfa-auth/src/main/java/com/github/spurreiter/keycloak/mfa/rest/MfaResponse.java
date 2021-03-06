package com.github.spurreiter.keycloak.mfa.rest;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MfaResponse {
    private static String ERROR = "error";

    // error codes from response
    public static String ERR_EXPIRED = "mfa_expired";
    public static String ERR_SERVER_ERROR = "server_error";
    public static String ERR_TMP_UNAVAILABLE = "temporarily_unavailable";
    public static String ERR_INVALID = "mfa_invalid";
    public static String ERR_INVALID_ID = "invalid_id";
    public static String ERR_MAX_RETRIES = "max_retries";
    public static String ERR_MAX_VERIFIED = "max_verified";
    public static String ERR_MISSING_ID = "missing_id";

    private Response response;

    private boolean isNonceInvalid = false;

    public MfaResponse() {
    }

    public MfaResponse(Response response) {
        this.response = response;
    }

    public Integer getStatus() {
        if (response == null) {
            return 500;
        }
        return response.getStatus();
    }

    public String getError() {
        if (isNonceInvalid) {
            return ERR_SERVER_ERROR;
        }
        if (response == null) {
            return ERR_TMP_UNAVAILABLE;
        } else if (response.getStatus() < 400) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.readEntity(String.class));
            return json.get(ERROR).asText();
        } catch (Exception e) {
        }
        return ERR_TMP_UNAVAILABLE;
    }

    public boolean verifyNonce(String nonce) {
        if (response == null || response.getStatus() >= 400) {
            return false;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.readEntity(String.class));
            isNonceInvalid = !nonce.equals(json.get("nonce").asText());
            return isNonceInvalid;
        } catch (Exception e) {
        }
        return false;
    }
}
