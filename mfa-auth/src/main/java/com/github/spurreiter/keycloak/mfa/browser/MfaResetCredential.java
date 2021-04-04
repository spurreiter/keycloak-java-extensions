/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.spurreiter.keycloak.mfa.browser;

import org.keycloak.authentication.actiontoken.DefaultActionTokenKey;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.Time;
import org.keycloak.credential.*;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.github.spurreiter.keycloak.mfa.rest.MfaRequest;
import com.github.spurreiter.keycloak.mfa.rest.MfaResponse;
import com.github.spurreiter.keycloak.mfa.util.MfaHelper;
import com.github.spurreiter.keycloak.mfa.util.UserAttributes;

import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;

import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT;
import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT_USER;
import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT_PWD;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.BOOLEAN_TYPE;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MfaResetCredential implements Authenticator, AuthenticatorFactory {

    private static final Logger logger = Logger.getLogger(MfaResetCredential.class);

    public static final String REST_ENDPOINT_EMAIL = "restEndpointEmail";
    public static final String REST_ENDPOINT_EMAIL_USER = "restEndpointEmailUser";
    public static final String REST_ENDPOINT_EMAIL_PWD = "restEndpointEmailPwd";
    private static final String USE_REALM_EMAIL_PROVIDER = "useRealmEmailProvider";
    private static final String MFA_CREDENTIAL_TYPE = "MfaResetCredential";

    public static final String MFA_CHALLENGE_SENT = "mfaChallengeSent";
    public static final String PROVIDER_ID = "reset-credential-mfa";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        UserModel user = context.getUser();

        // we don't want people guessing usernames, so if there was a problem obtaining
        // the user, the user will be null.
        // just reset login for with a success message
        if (user == null) {
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }
        if (handleActionToken(context)) {
            return;
        }

        logger.infof("foobar=%s", MfaHelper.getConfig(context).get(USE_REALM_EMAIL_PROVIDER));

        EventBuilder event = context.getEvent();
        // we don't want people guessing usernames, so if there is a problem, just
        // continuously challenge
        if (user.getEmail() == null || user.getEmail().trim().length() == 0) {
            if (UserAttributes.getString(user, UserAttributes.PHONE_NUMBER) != null) {
                authenticateMfa(context);
                return;
            }
            event.user(user).detail(Details.USERNAME, user.getUsername()).error(Errors.INVALID_EMAIL);
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
            return;
        }

        authenticateEmail(context);
    }

    private void authenticateEmail(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String username = authenticationSession.getAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME);
        EventBuilder event = context.getEvent();

        int validityInSecs = context.getRealm()
                .getActionTokenGeneratedByUserLifespan(ResetCredentialsActionToken.TOKEN_TYPE);
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;

        // We send the secret in the email in a link as a query param.
        String authSessionEncodedId = AuthenticationSessionCompoundId.fromAuthSession(authenticationSession)
                .getEncodedId();
        ResetCredentialsActionToken token = new ResetCredentialsActionToken(user.getId(), absoluteExpirationInSecs,
                authSessionEncodedId, authenticationSession.getClient().getClientId());
        String link = UriBuilder
                .fromUri(context.getActionTokenUrl(
                        token.serialize(context.getSession(), context.getRealm(), context.getUriInfo())))
                .build().toString();
        long expirationInMinutes = TimeUnit.SECONDS.toMinutes(validityInSecs);
        
        try {
            if (MfaHelper.getConfig(context).get(USE_REALM_EMAIL_PROVIDER) == "true") {
                sendEmailWithSmtp(context, link, expirationInMinutes);
            } else {
                sendEmailWithRest(context, link, expirationInMinutes);
            }

            event.clone().event(EventType.SEND_RESET_PASSWORD).user(user).detail(Details.USERNAME, username)
                    .detail(Details.EMAIL, user.getEmail())
                    .detail(Details.CODE_ID, authenticationSession.getParentSession().getId()).success();
            context.forkWithSuccessMessage(new FormMessage(Messages.EMAIL_SENT));
        } catch (EmailException e) {
            event.clone().event(EventType.SEND_RESET_PASSWORD).detail(Details.USERNAME, username).user(user)
                    .error(Errors.EMAIL_SEND_FAILED);
            ServicesLogger.LOGGER.failedToSendPwdResetEmail(e);
            Response challenge = context.form().setError(Messages.EMAIL_SENT_ERROR)
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
    }

    // handle reset action token sent by email
    private boolean handleActionToken(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        String actionTokenUserId = authenticationSession.getAuthNote(DefaultActionTokenKey.ACTION_TOKEN_USER_ID);
        if (actionTokenUserId != null && Objects.equals(user.getId(), actionTokenUserId)) {
            logger.debugf(
                    "Forget-password triggered when reauthenticating user after authentication via action token. Skipping "
                            + PROVIDER_ID + " screen and using user '%s' ",
                    user.getUsername());
            context.getUser().setEmailVerified(true);
            authenticationSession.setAuthNote(MFA_CREDENTIAL_TYPE, "email");
            context.success();
            return true;
        }
        return false;
    }

    private void sendEmailWithSmtp(AuthenticationFlowContext context, String link, long expirationInMinutes)
            throws EmailException {
        UserModel user = context.getUser();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        context.getSession().getProvider(EmailTemplateProvider.class).setRealm(context.getRealm()).setUser(user)
                .setAuthenticationSession(authenticationSession).sendPasswordReset(link, expirationInMinutes);

    }

    private void sendEmailWithRest(AuthenticationFlowContext context, String link, long expirationInMinutes)
            throws EmailException {
        Map<String, String> config = MfaHelper.getConfig(context);
        String url = config.get(REST_ENDPOINT_EMAIL);
        String username = config.get(REST_ENDPOINT_EMAIL_USER);
        String password = config.get(REST_ENDPOINT_EMAIL_PWD);
        Map<String, List<String>> userAttributes = context.getUser().getAttributes();
        MfaResponse response = MfaRequest.buildRequest(context, url, username, password).sendResetEmail(userAttributes,
                link, expirationInMinutes);
        String error = response.getError();
        if (error != null) {
            throw new EmailException(error);
        }
    }

    private void authenticateMfa(AuthenticationFlowContext context) {
        new MfaForm().requestChallenge(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (authSession.getAuthNote(MFA_CREDENTIAL_TYPE) == "email") {
            authSession.removeAuthNote(MFA_CREDENTIAL_TYPE);
            context.success();
            return;
        }
        new MfaForm().action(context);
    }

    public static Long getLastChangedTimestamp(KeycloakSession session, RealmModel realm, UserModel user) {
        // TODO(hmlnarik): Make this more generic to support non-password credential
        // types
        PasswordCredentialProvider passwordProvider = (PasswordCredentialProvider) session
                .getProvider(CredentialProvider.class, PasswordCredentialProviderFactory.PROVIDER_ID);
        CredentialModel password = passwordProvider.getPassword(realm, user);

        return password == null ? null : password.getCreatedDate();
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public String getDisplayType() {
        return "MFA Send Reset Email";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Send email via REST service to user and wait for response.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()

                .property().name(REST_ENDPOINT).label("REST endpoint")
                .helpText("REST endpoint to send the OTP for reset. "
                        + "If EnvVar MFA_URL is defined a relative URL can be set.")
                .type(STRING_TYPE).defaultValue("http://localhost:1080/mfa").add()

                .property().name(REST_ENDPOINT_USER).label("Username").helpText("Basic-auth Username for REST endpoint")
                .type(STRING_TYPE).add()

                .property().name(REST_ENDPOINT_PWD).label("Password").helpText("Basic-auth Password for REST endpoint")
                .type(STRING_TYPE).add()

                .property().name(USE_REALM_EMAIL_PROVIDER).label("Use Realm Email Provider")
                .helpText("Use Realm Email Provider instead of REST Endpoint (Email).").type(BOOLEAN_TYPE).add()

                .property().name(REST_ENDPOINT_EMAIL).label("REST endpoint (Email)")
                .helpText("REST endpoint to send the OTP for reset. "
                        + "If EnvVar MFA_URL is defined a relative URL can be set.")
                .type(STRING_TYPE).defaultValue("http://localhost:1080/mfa").add()

                .property().name(REST_ENDPOINT_EMAIL_USER).label("Username (Email)")
                .helpText("Basic-auth Username for REST endpoint (Email)").type(STRING_TYPE).add()

                .property().name(REST_ENDPOINT_EMAIL_PWD).label("Password (Email)")
                .helpText("Basic-auth Password for REST endpoint (Email)").type(STRING_TYPE).add()

                .build();
    }

    @Override
    public void close() {

    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
