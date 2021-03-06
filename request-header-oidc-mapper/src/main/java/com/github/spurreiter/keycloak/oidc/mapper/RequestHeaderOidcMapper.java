package com.github.spurreiter.keycloak.oidc.mapper;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.ClientAuthUtil;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.representations.IDToken;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class RequestHeaderOidcMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final String PROVIDER_ID = "request-header-oidc-mapper";

    private static final Logger log = Logger.getLogger(RequestHeaderOidcMapper.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    private static final String CONFIG_PROPERTY = "configProperty";

    static {

        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()

                .property().name(CONFIG_PROPERTY).type(ProviderConfigProperty.STRING_TYPE).label("Request Header")
                .helpText("Request Header Name to map into token").defaultValue("X-Request").add()

                .property().name(ProtocolMapperUtils.MULTIVALUED).label(ProtocolMapperUtils.MULTIVALUED_LABEL)
                .helpText(ProtocolMapperUtils.MULTIVALUED_HELP_TEXT).type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false).add()

                .build();

        OIDCAttributeMapperHelper.addAttributeConfig(CONFIG_PROPERTIES, UserPropertyMapper.class);
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "Request Header Mapper";
    }

    @Override
    public String getHelpText() {
        return "Oidc token mapper for request header";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {

        String requestHeaderName = mappingModel.getConfig().getOrDefault(CONFIG_PROPERTY, "defaultProperty");

        List<String> headers = keycloakSession.getContext().getRequestHeaders().getRequestHeader(requestHeaderName);

        String mapperName = mappingModel.getName();
        String clientId = keycloakSession.getContext().getClient().getClientId();

        try {
            String claimValue = headers.get(0);
            log.infof("mapClaim mapper=%s %s=%s clientid=%s", mapperName,
                    mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME), claimValue, clientId);
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, claimValue);
        } catch (Exception e) {
            log.warnf("header missing. mapper=%s clientid=%s", mapperName, clientId);
            Response response = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(),
                    OAuthErrorException.INVALID_REQUEST, "Header missing");

            throw new WebApplicationException(response);
        }
    }
}
