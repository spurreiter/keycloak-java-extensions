package com.github.spurreiter.keycloak.mfa.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.spurreiter.keycloak.mfa.rest.MfaRequest;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MfaHelper {
    // configuration
    public static final String REST_ENDPOINT = "restEndpoint";
    public static final String REST_ENDPOINT_USER = "restEndpointUser";
    public static final String REST_ENDPOINT_PWD = "restEndpointPwd";
    public static final String OTP_AUTH_KEY = "otpAuthAttribute";
    public static final String OTP_ROLE_KEY = "otpAuthRole";

    public static MfaRequest getMfaRequest(AuthenticationFlowContext context) {
        Map<String, String> config = MfaHelper.getConfig(context);
        String url = config.get(REST_ENDPOINT);
        String basicUser = config.get(REST_ENDPOINT_USER);
        String basicPass = config.get(REST_ENDPOINT_PWD);
        return new MfaRequest(url).setBasicAuth(basicUser, basicPass).setRequestId(getRequestId(context));
    }

    public static String getRequestId(AuthenticationFlowContext context) {
        try {
            return context.getHttpRequest().getHttpHeaders().getRequestHeader("x-request-id").get(0);
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean matchCondition(AuthenticationFlowContext context) {
        boolean useOtp = false;
        UserModel user = context.getUser();
        RealmModel realm = context.getRealm();

        Map<String, String> config = getConfig(context);
        String attribute = config.get(OTP_AUTH_KEY);
        String roleName = config.get(OTP_ROLE_KEY);

        if (attribute != null) {
            useOtp = user.getAttributeStream(attribute).findFirst().isPresent();
        }

        if (!useOtp && roleName != null) {
            RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);
            if (role != null) {
                useOtp = user.hasRole(role);
            }
        }
        return useOtp;
    }

    public static Map<String, String> getConfig(AuthenticationFlowContext context) {
        return context.getAuthenticatorConfig() == null ? Collections.emptyMap()
                : context.getAuthenticatorConfig().getConfig();
    }

}
