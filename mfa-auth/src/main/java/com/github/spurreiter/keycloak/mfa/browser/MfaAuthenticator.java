package com.github.spurreiter.keycloak.mfa.browser;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.UserModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class MfaAuthenticator implements Authenticator {

    // private static final Logger logger = Logger.getLogger(MfaForm.class);

    public static final String MFA_CHALLENGE_SENT = "mfaChallengeSent";
    public static final String MFA_CHALLENGE_START = "mfaChallengeStart";
    public static final String OTP_AUTH = "otpauth";
    public static final String OTP_ROLE = "otp:auth";

    // 1st step
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        new MfaForm().authenticate(context);
    }

    // 2nd step
    @Override
    public void action(AuthenticationFlowContext context) {
        new MfaForm().action(context);
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
