package com.github.spurreiter.keycloak.mfa.directgrant;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.github.spurreiter.keycloak.mfa.rest.MfaResponse;
import com.github.spurreiter.keycloak.mfa.util.MfaHelper;

import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.Theme;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.ROLE_TYPE;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;

import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.REST_ENDPOINT;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.REST_ENDPOINT_USER;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.REST_ENDPOINT_PWD;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.OTP_AUTH_KEY;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.OTP_ROLE_KEY;

public class MfaDirectGrantAuthenticator extends AbstractDirectGrantAuthenticator {
    public static final String PROVIDER_ID = "auth-mfa-directgrant";

    private static final Logger log = Logger.getLogger(MfaDirectGrantAuthenticator.class);

    public static final String OTP_AUTH = "otpAuth";
    public static final String OTP_ROLE = "otp:auth";

    public static final String MFA_CHALLENGE_START = "mfaChallengeStart";

    // 1st step
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (!MfaHelper.matchCondition(context)) {
            context.success();
            return;
        }

        MultivaluedMap<String, String> inputData = context.getHttpRequest().getDecodedFormParameters();

        String otp = inputData.getFirst("otp");

        RealmModel realm = context.getRealm();
        String realmname = realm.getName();
        UserModel user = context.getUser();
        String username = user.getUsername();

        if (otp == null || otp.isEmpty()) {
            log.infof("authenticate for username=%s", username);
            MfaResponse response = MfaHelper.getMfaRequest(context).send(context.getUser().getAttributes());
            String error = response.getError();

            if (error == null) {
                error = "mfa_sent";
                log.infof("Mfa sent. realm=%s username=%s", realmname, username);
            } else {
                log.warnf("Mfa failed. requestid=%s realm=%s username=%s error=%s", MfaHelper.getRequestId(context),
                        realmname, username, error);
            }
            challengeResponseFailed(context, error, getLocaleError(context, error));
            return;
        }
        MfaResponse response = MfaHelper.getMfaRequest(context).verify(context.getUser().getAttributes(), otp);
        String error = response.getError();

        if (error != null) {
            log.warnf("authentication failed. requestid=%s realm=%s username=%s error=%s",
                    MfaHelper.getRequestId(context), realmname, username, error);
            if (MfaResponse.ERR_INVALID.equals(error) || MfaResponse.ERR_TMP_UNAVAILABLE.equals(error)
                    || MfaResponse.ERR_SERVER_ERROR.equals(error)) {
                challengeResponseFailed(context, error, getLocaleError(context, error));
            } else {
                challengeResponseFailed(context);
            }
            return;
        }

        log.infof("authentication successful. realm=%s username=%s", realmname, username);
        context.success();
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getDisplayType() {
        return "Mfa Direct Grant";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates the one time password supplied as a 'otp' form parameter in direct grant request";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty restEndpoint = new ProviderConfigProperty();
        restEndpoint.setType(STRING_TYPE);
        restEndpoint.setName(REST_ENDPOINT);
        restEndpoint.setLabel("REST endpoint");
        restEndpoint.setHelpText("REST endpoint to send OTP.");
        restEndpoint.setDefaultValue("http://localhost:1080/mfa");

        ProviderConfigProperty restEndpointUser = new ProviderConfigProperty();
        restEndpointUser.setType(STRING_TYPE);
        restEndpointUser.setName(REST_ENDPOINT_USER);
        restEndpointUser.setLabel("Username");
        restEndpointUser.setHelpText("Basic-auth Username for REST endpoint");

        ProviderConfigProperty restEndpointPwd = new ProviderConfigProperty();
        restEndpointPwd.setType(STRING_TYPE);
        restEndpointPwd.setName(REST_ENDPOINT_PWD);
        restEndpointPwd.setLabel("Password");
        restEndpointPwd.setHelpText("Basic-auth Password for REST endpoint");

        ProviderConfigProperty forceOtpUserAttribute = new ProviderConfigProperty();
        forceOtpUserAttribute.setType(STRING_TYPE);
        forceOtpUserAttribute.setName(OTP_AUTH_KEY);
        forceOtpUserAttribute.setLabel("OTP control User Attribute");
        forceOtpUserAttribute.setHelpText("The name of the user attribute to explicitly control OTP auth.");
        forceOtpUserAttribute.setDefaultValue(OTP_AUTH);

        ProviderConfigProperty forceOtpRole = new ProviderConfigProperty();
        forceOtpRole.setType(ROLE_TYPE);
        forceOtpRole.setName(OTP_ROLE_KEY);
        forceOtpRole.setLabel("Force OTP for Role");
        forceOtpRole.setHelpText("OTP is always required if user has the given Role.");
        forceOtpRole.setDefaultValue(OTP_ROLE);

        return asList(restEndpoint, restEndpointUser, restEndpointPwd, forceOtpUserAttribute, forceOtpRole);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private void challengeResponseFailed(AuthenticationFlowContext context) {
        challengeResponseFailed(context, "invalid_grant", "Invalid user credentials");
    }

    private void challengeResponseFailed(AuthenticationFlowContext context, String error, String description) {
        context.getEvent().user(context.getUser());
        context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
        Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), error, description);
        context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
    }

    private String getLocaleError(AuthenticationFlowContext context, String error) {
        if (error == null) {
            return null;
        }
        KeycloakSession session = context.getSession();

        String msg = null;
        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = new Locale("en");
            msg = (String) theme.getMessages(locale).getProperty("mfaError." + error);
        } catch (IOException e) {
            log.error(e.toString());
        }
        if (msg == null) {
            msg = Messages.FAILED_TO_PROCESS_RESPONSE;
        }
        // log.info(msg);
        return msg;
    }
}
