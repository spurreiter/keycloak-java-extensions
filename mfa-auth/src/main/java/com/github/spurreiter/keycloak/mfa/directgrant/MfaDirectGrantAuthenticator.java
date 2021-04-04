package com.github.spurreiter.keycloak.mfa.directgrant;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.directgrant.AbstractDirectGrantAuthenticator;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.github.spurreiter.keycloak.mfa.rest.MfaRequest;
import com.github.spurreiter.keycloak.mfa.rest.MfaResponse;
import com.github.spurreiter.keycloak.mfa.util.MfaHelper;

import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;
import org.keycloak.theme.Theme;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.ROLE_TYPE;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT;
import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT_USER;
import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT_PWD;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.OTP_AUTH_KEY;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.OTP_ROLE_KEY;

public class MfaDirectGrantAuthenticator extends AbstractDirectGrantAuthenticator {
    public static final String PROVIDER_ID = "auth-mfa-directgrant";

    private static final Logger logger = Logger.getLogger(MfaDirectGrantAuthenticator.class);

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
            logger.infof("authenticate for username=%s", username);
            MfaResponse response = MfaRequest.buildRequest(context).send(context.getUser().getAttributes());
            String error = response.getError();

            if (error == null) {
                error = "mfa_sent";
                logger.infof("Mfa sent. realm=%s username=%s", realmname, username);
            } else {
                logger.warnf("Mfa failed. requestid=%s realm=%s username=%s error=%s", MfaHelper.getRequestId(context),
                        realmname, username, error);
            }
            challengeResponseFailed(context, error, getLocaleError(context, error));
            return;
        }
        MfaResponse response = MfaRequest.buildRequest(context).verify(context.getUser().getAttributes(), otp);
        String error = response.getError();

        if (error != null) {
            logger.warnf("authentication failed. requestid=%s realm=%s username=%s error=%s",
                    MfaHelper.getRequestId(context), realmname, username, error);
            if (MfaResponse.ERR_INVALID.equals(error) || MfaResponse.ERR_TMP_UNAVAILABLE.equals(error)
                    || MfaResponse.ERR_SERVER_ERROR.equals(error)) {
                challengeResponseFailed(context, error, getLocaleError(context, error));
            } else {
                challengeResponseFailed(context);
            }
            return;
        }

        logger.infof("authentication successful. realm=%s username=%s", realmname, username);
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
        return "MFA Direct Grant";
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
        return ProviderConfigurationBuilder.create()

                .property().name(REST_ENDPOINT).label("REST endpoint")
                .helpText("REST endpoint to send the email for verification. "
                        + "If EnvVar MFA_URL is defined a relative URL can be set.")
                .type(STRING_TYPE).defaultValue("http://localhost:1080/mfa").add()

                .property().name(REST_ENDPOINT_USER).label("Username")
                .helpText("Basic-auth Username for REST endpoint. EnvVar MFA_USERNAME is used alternatively.")
                .type(STRING_TYPE).add()

                .property().name(REST_ENDPOINT_PWD).label("Password")
                .helpText("Basic-auth Password for REST endpoint. EnvVar MFA_PASSWORD is used alternatively.")
                .type(STRING_TYPE).add()

                .property().name(OTP_AUTH_KEY).label("OTP control User Attribute")
                .helpText("The name of the user attribute to explicitly control OTP auth.").type(STRING_TYPE)
                .defaultValue(OTP_AUTH).add()

                .property().name(OTP_ROLE_KEY).label("Force OTP for Role")
                .helpText("OTP is always required if user has the given Role.").type(ROLE_TYPE).add()

                .build();
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
            logger.error(e.toString());
        }
        if (msg == null) {
            msg = Messages.FAILED_TO_PROCESS_RESPONSE;
        }
        // log.info(msg);
        return msg;
    }
}
