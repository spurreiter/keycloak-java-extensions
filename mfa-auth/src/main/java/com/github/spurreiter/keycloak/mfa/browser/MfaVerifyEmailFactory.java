package com.github.spurreiter.keycloak.mfa.browser;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
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
        //NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //NOOP
    }

    @Override
    public void close() {
        //NOOP
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
        ProviderConfigProperty restEndpoint = new ProviderConfigProperty();
        restEndpoint.setType(STRING_TYPE);
        restEndpoint.setName(REST_ENDPOINT);
        restEndpoint.setLabel("REST endpoint");
        restEndpoint.setHelpText("REST endpoint to send OTP.");
        restEndpoint.setDefaultValue("http://localhost:1080/mfa/verify-email");

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

        ProviderConfigProperty forceOtpRole = new ProviderConfigProperty();
        forceOtpRole.setType(ROLE_TYPE);
        forceOtpRole.setName(OTP_ROLE_KEY);
        forceOtpRole.setLabel("Force OTP for Role");
        forceOtpRole.setHelpText("OTP is always required if user has the given Role.");

        return asList(restEndpoint, restEndpointUser, restEndpointPwd, forceOtpUserAttribute, forceOtpRole);
    }
}
