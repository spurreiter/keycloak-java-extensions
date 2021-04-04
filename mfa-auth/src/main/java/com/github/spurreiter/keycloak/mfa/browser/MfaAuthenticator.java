package com.github.spurreiter.keycloak.mfa.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticator;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.Locale;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.github.spurreiter.keycloak.mfa.rest.MfaRequest;
import com.github.spurreiter.keycloak.mfa.rest.MfaResponse;
import com.github.spurreiter.keycloak.mfa.util.MfaHelper;
import com.github.spurreiter.keycloak.mfa.util.UserAttributes;

import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MfaAuthenticator extends OTPFormAuthenticator {

    private static final Logger logger = Logger.getLogger(MfaAuthenticator.class);

    public static final String MFA_CHALLENGE_SENT = "mfaChallengeSent";
    public static final String OTP_AUTH = "otpauth";
    public static final String OTP_ROLE = "otp:auth";

    public static final String MFA_CHALLENGE_START = "mfaChallengeStart";

    private KeycloakSession session;

    public MfaAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    // 1st step
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String username = user.getUsername();

        boolean needsOtpAuth = MfaHelper.matchCondition(context);

        if (!needsOtpAuth && UserAttributes.isPhoneVerifiedOrNull(user)) {
            context.success();
            return;
        }

        logger.infof("authenticate for username=%s", username);

        requestChallenge(context, username, context.getAuthenticationSession());
    }

    // 2nd step
    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel")) {
            context.resetFlow();
            context.fork();
            return;
        }

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        String username = user.getUsername();
        String xRequestId = MfaHelper.getRequestId(context);
        boolean doExit = false;

        logger.infof("action. requestid=%s realm=%s username=%s", xRequestId, realm.getName(), username);

        MfaResponse response;
        String error;

        if (formData.containsKey("resend")) {
            response = MfaRequest.buildRequest(context).send(context.getUser().getAttributes());
            error = response.getError();
            doExit = error != null;
        } else {
            String code = formData.getFirst("challenge_input");
            response = MfaRequest.buildRequest(context).verify(context.getUser().getAttributes(), code);
            error = response.getError();
            doExit = !MfaResponse.ERR_INVALID.equals(error);

            // success
            if (error == null) {
                if (!UserAttributes.isPhoneVerified(user)) {
                    user.setSingleAttribute(UserAttributes.PHONE_NUMBER_VERIFIED, "true");                       
                }
                logger.infof("authentication successful. realm=%s username=%s", realm.getName(), username);
                context.getAuthenticationSession().removeAuthNote(MFA_CHALLENGE_SENT);
                context.success();
                return;
            }
        }

        String errorMessage = getLocaleError(context, error);

        // failed -> exit
        if (doExit) {
            logger.infof("authentication failed. requestid=%s realm=%s username=%s error=%s", xRequestId, realm.getName(),
                    user.getUsername(), error);
            context.getEvent().user(user);

            if (MfaResponse.ERR_EXPIRED.equals(error)) {
                context.getEvent().error(Errors.SESSION_EXPIRED);
            } else {
                context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            }
            context.resetFlow();
            context.forkWithErrorMessage(new FormMessage(errorMessage));
            return;
        }

        // failed -> retry
        logger.infof("authentication attempt failed. Retrying requestid=%s realm=%s username=%s error=%s", xRequestId,
                realm.getName(), user.getUsername(), error);

        Response formResponse = createChallengeFormResponse(context, error);
        context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, formResponse);
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
    public void close() {
        // NOOP
    }

    private void requestChallenge(AuthenticationFlowContext context, String username,
            AuthenticationSessionModel authSession) {
        String error = null;

        if (authSession.getAuthNote(MFA_CHALLENGE_SENT) == null) {
            authSession.setAuthNote(MFA_CHALLENGE_SENT, MFA_CHALLENGE_SENT);
            MfaResponse response = MfaRequest.buildRequest(context).send(context.getUser().getAttributes());
            error = response.getError();
        }

        if (error == null) {
            Response formResponse = createChallengeFormResponse(context, error);
            context.challenge(formResponse);
            return;
        }

        logger.warnf("requestChallenge failed. requestId=%s username=%s error=%s", MfaHelper.getRequestId(context),
                username, error);
        String errorMessage = getLocaleError(context, error);
        context.forkWithErrorMessage(new FormMessage(errorMessage));
    }

    private Response createChallengeFormResponse(AuthenticationFlowContext context, String error) {
        LoginFormsProvider form = context.form();

        if (error != null) {
            if (MfaResponse.ERR_INVALID.equals(error)) {
                form.setError(Messages.INVALID_TOTP);
            } else {
                String errorMessage = getLocaleError(context, error);
                form.setError(errorMessage);
            }
        }

        return form.createForm("mfa-auth-form-otp.ftl");
    }

    private String getLocaleError(AuthenticationFlowContext context, String error) {
        if (error == null) {
            return null;
        }

        String msg = null;
        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(context.getUser());
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
