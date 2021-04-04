package com.github.spurreiter.keycloak.mfa.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.ROLE_TYPE;

import java.util.List;
import static java.util.Arrays.asList;

import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT;
import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT_USER;
import static com.github.spurreiter.keycloak.mfa.rest.MfaRequest.REST_ENDPOINT_PWD;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.OTP_AUTH_KEY;
import static com.github.spurreiter.keycloak.mfa.util.MfaHelper.OTP_ROLE_KEY;

public class MfaVerifyEmailFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "verify-email-mfa-form";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new MfaVerifyEmail(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
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
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getDisplayType() {
        return "MFA Verify Email";
    }

    @Override
    public String getHelpText() {
        return "Verifies Email.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()

                .property().name(REST_ENDPOINT).label("REST endpoint")
                .helpText("REST endpoint to send the email for verification. "
                        + "If EnvVar MFA_URL is defined a relative URL can be set.")
                .type(STRING_TYPE).defaultValue("http://localhost:1080/mfa/verify-email").add()

                .property().name(REST_ENDPOINT_USER).label("Username")
                .helpText("Basic-auth Username for REST endpoint. EnvVar MFA_USERNAME is used alternatively.")
                .type(STRING_TYPE).add()

                .property().name(REST_ENDPOINT_PWD).label("Password")
                .helpText("Basic-auth Password for REST endpoint. EnvVar MFA_PASSWORD is used alternatively.")
                .type(STRING_TYPE).add()

                .build();
    }
}
