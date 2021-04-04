package com.github.spurreiter.keycloak.mfa.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.authentication.Authenticator;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.github.spurreiter.keycloak.mfa.rest.MfaRequest;
import com.github.spurreiter.keycloak.mfa.rest.MfaResponse;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MfaVerifyEmail implements Authenticator {

    private static final Logger log = Logger.getLogger(MfaVerifyEmail.class);

    public static final String MFA_CHALLENGE_SENT = "mfaChallengeSent";

    public static final String MFA_CHALLENGE_START = "mfaChallengeStart";

    private KeycloakSession session;

    public MfaVerifyEmail(KeycloakSession session) {
        this.session = session;
    }

    private String getVerifyEmailToken(AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        UriInfo uriInfo = context.getSession().getContext().getUri();

        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(VerifyEmailActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authSession).getEncodedId();
        VerifyEmailActionToken token = new VerifyEmailActionToken(user.getId(), absoluteExpirationInSecs,
                authSessionEncodedId, user.getEmail(), authSession.getClient().getClientId());
        UriBuilder builder = Urls.actionTokenBuilder(uriInfo.getBaseUri(), token.serialize(session, realm, uriInfo),
                authSession.getClient().getClientId(), authSession.getTabId());
        String link = builder.build(realm.getName()).toString();
        return link;
    }

    private long getExpirationInMinutes(RealmModel realm) {
        int validityInSecs = realm.getActionTokenGeneratedByUserLifespan(VerifyEmailActionToken.TOKEN_TYPE);
        return TimeUnit.SECONDS.toMinutes(validityInSecs);
    }

    private void challengeFormResponse(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String email = user.getEmail();

        EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, email);
        LoginFormsProvider form = context.form();

        try {
            String link = getVerifyEmailToken(context);
            log.infof("token=%s", link);
            MfaResponse response = MfaRequest.buildRequest(context).sendVerifyEmail(user.getAttributes(), link,
                    getExpirationInMinutes(context.getRealm()));
            String error = response.getError();
            if (error == null) {
                event.success();
            } else {
                throw new Exception(error);
            }
        } catch (Exception e) {
            log.error("Failed to generate email verification link", e);
            event.error(Errors.EMAIL_SEND_FAILED);
            form.setError(Messages.EMAIL_SENT_ERROR);
        }
        Response response = form.createForm("mfa-login-verify-email.ftl");
        context.challenge(response);
    }

    // 1st step
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String email = user.getEmail();

        log.infof("user=%s email=%s emailv=%s", user.getUsername(), user.getEmail(), user.isEmailVerified());

        if (email != null && !user.isEmailVerified()) {
            challengeFormResponse(context);
            return;
        }

        context.success();
    }

    // 2nd step
    @Override
    public void action(AuthenticationFlowContext context) {
        // MultivaluedMap<String, String> formData =
        // context.getHttpRequest().getDecodedFormParameters();

        // if (formData.containsKey("cancel")) {
        // context.resetFlow();
        // context.fork();
        // return;
        // }

        challengeFormResponse(context);
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
}
