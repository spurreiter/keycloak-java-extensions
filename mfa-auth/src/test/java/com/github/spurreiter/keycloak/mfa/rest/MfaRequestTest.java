package com.github.spurreiter.keycloak.mfa.rest;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

public class MfaRequestTest {
    private static String URL = "http://localhost:1080/mfa";

    private static Map<String, List<String>> getAttributes () {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("email", new ArrayList<>(Arrays.asList("test@test.test")));
        attributes.put("emailVerified", new ArrayList<>(Arrays.asList("true")));
        return attributes;
    }


    @Injectable
    private Response response;

    @Test
    public void initiateMfa() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {{
            response.getStatus(); 
            result = 201;
        }};

        MfaRequest sut = new MfaRequest(URL);
        assertTrue(sut.send(getAttributes()).getStatus() == Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void initiateMfaBasicAuth() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {
            {
                response.getStatus();
                result = 201;
            }
        };

        MfaRequest sut = new MfaRequest(URL).setBasicAuth("user", "test");
        assertTrue(sut.send(getAttributes()).getStatus() == Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void initiateMfaFails() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {
            {
                response.getStatus();
                result = 404;
            }
        };

        MfaRequest sut = new MfaRequest(URL).setBasicAuth("user", "test");
        assertFalse(sut.send(getAttributes()).getStatus() == Response.Status.CREATED.getStatusCode());
    }

    @Test
    public void retryMfa() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {
            {
                response.getStatus();
                result = 200;
            }
        };

        MfaRequest sut = new MfaRequest(URL);
        MfaResponse response = sut.send(getAttributes());

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(null, response.getError());
    }

    @Test
    public void retryMfaFail() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {
            {
                response.getStatus();
                result = Response.Status.NOT_FOUND.getStatusCode();
                response.readEntity(String.class);
                result = "{\"status\": 404,\"error\":\"max_retries\"}";
            }
        };


        MfaRequest sut = new MfaRequest(URL);
        MfaResponse response = sut.send(getAttributes());
        // System.out.println(MfaResponse.getError(response));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        assertEquals(MfaResponse.ERR_MAX_RETRIES, response.getError());
    }

    @Test
    public void verifyMfa() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {
            {
                response.getStatus();
                result = 200;
            }
        };

        MfaRequest sut = new MfaRequest(URL);
        assertTrue(sut.verify(getAttributes(), "438804").getStatus() == Response.Status.OK.getStatusCode());
    }

    @Test
    public void verifyMfaBadNonce() {
        new MockUp<MfaRequest>() {
            @Mock
            Response request(String method, String path, Entity<?> entity) {
                return response;
            }
        };

        new Expectations() {
            {
                response.getStatus();
                result = 200;
                response.readEntity(String.class);
                result = "{\"nonce\":\"failed60-be54-4994-a680-5c9f5fa56cb2\"}";
            }
        };

        MfaRequest sut = new MfaRequest(URL);
        MfaResponse response = sut.verify(getAttributes(), "438804");
        assertEquals(200, response.getStatus());
        assertEquals("server_error", response.getError());
    }
}
