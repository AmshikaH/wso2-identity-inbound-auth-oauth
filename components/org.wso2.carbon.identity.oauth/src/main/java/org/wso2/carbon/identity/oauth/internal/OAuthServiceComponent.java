/*
 * Copyright (c) 2013-2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.action.execution.api.service.ActionExecutionRequestBuilder;
import org.wso2.carbon.identity.action.execution.api.service.ActionExecutionResponseProcessor;
import org.wso2.carbon.identity.action.execution.api.service.ActionExecutorService;
import org.wso2.carbon.identity.application.authentication.framework.UserSessionManagementService;
import org.wso2.carbon.identity.application.mgt.ApplicationManagementService;
import org.wso2.carbon.identity.application.mgt.AuthorizedAPIManagementService;
import org.wso2.carbon.identity.application.mgt.inbound.protocol.ApplicationInboundAuthConfigHandler;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.core.util.IdentityCoreInitializedEvent;
import org.wso2.carbon.identity.cors.mgt.core.CORSManagementService;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.oauth.OAuthAdminServiceImpl;
import org.wso2.carbon.identity.oauth.OauthInboundAuthConfigHandler;
import org.wso2.carbon.identity.oauth.action.execution.PreIssueAccessTokenRequestBuilder;
import org.wso2.carbon.identity.oauth.action.execution.PreIssueAccessTokenResponseProcessor;
import org.wso2.carbon.identity.oauth.action.rule.PreIssueAccessTokenRuleEvaluationDataProvider;
import org.wso2.carbon.identity.oauth.cache.OAuthCache;
import org.wso2.carbon.identity.oauth.common.OAuthConstants;
import org.wso2.carbon.identity.oauth.common.token.bindings.TokenBinderInfo;
import org.wso2.carbon.identity.oauth.config.OAuthServerConfiguration;
import org.wso2.carbon.identity.oauth.event.OAuthEventInterceptor;
import org.wso2.carbon.identity.oauth.listener.IdentityOathEventListener;
import org.wso2.carbon.identity.oauth.listener.IdentityOauthEventHandler;
import org.wso2.carbon.identity.oauth.listener.OAuthApplicationMgtListener;
import org.wso2.carbon.identity.oauth.listener.OAuthTokenSessionMappingEventHandler;
import org.wso2.carbon.identity.oauth2.OAuth2ScopeService;
import org.wso2.carbon.identity.oauth2.OAuth2Service;
import org.wso2.carbon.identity.oauth2.internal.OAuth2ServiceComponentHolder;
import org.wso2.carbon.identity.oauth2.token.handlers.response.AccessTokenResponseHandler;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.OrganizationUserSharingService;
import org.wso2.carbon.identity.organization.management.service.OrganizationManager;
import org.wso2.carbon.identity.organization.management.service.OrganizationUserResidentResolverService;
import org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service.OrgResourceResolverService;
import org.wso2.carbon.identity.role.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.rule.evaluation.api.provider.RuleEvaluationDataProvider;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * OAuth OSGi service component.
 */
@Component(
        name = "identity.oauth.component",
        immediate = true
)
public class OAuthServiceComponent {

    private static final Log log = LogFactory.getLog(OAuthServiceComponent.class);
    private ServiceRegistration serviceRegistration = null;

    protected void activate(ComponentContext context) {

        try {
            // initialize the OAuth Server configuration
            OAuthServerConfiguration oauthServerConfig = OAuthServerConfiguration.getInstance();

            if (OAuthCache.getInstance().isEnabled()) {
                log.debug("OAuth Caching is enabled. Initializing the cache.");
            }

            IdentityOathEventListener listener = new IdentityOathEventListener();
            serviceRegistration = context.getBundleContext().registerService(UserOperationEventListener.class.getName(),
                    listener, null);
            log.debug("Identity Oath Event Listener is enabled");

            context.getBundleContext().registerService(AbstractEventHandler.class.getName(),
                    new IdentityOauthEventHandler(), null);
            if (log.isDebugEnabled()) {
                log.debug("Identity Oauth Event handler is enabled");
            }

            OAuth2Service oauth2Service = new OAuth2Service();
            context.getBundleContext().registerService(OAuth2Service.class.getName(), oauth2Service, null);
            OAuthComponentServiceHolder.getInstance().setOauth2Service(oauth2Service);

            // We need to explicitly populate the OAuthTokenIssuerMap since it's used for token validation.
            oauthServerConfig.populateOAuthTokenIssuerMap();

            OAuthAdminServiceImpl oauthAdminService = new OAuthAdminServiceImpl();
            OAuthComponentServiceHolder.getInstance().setOAuthAdminService(oauthAdminService);
            OAuth2ServiceComponentHolder.getInstance().setOAuthAdminService(oauthAdminService);
            context.getBundleContext().registerService(OAuthEventInterceptor.class,
                    new OAuthTokenSessionMappingEventHandler(), null);
            if (log.isDebugEnabled()) {
                log.debug("OAuthTokenSessionMapping Event Handler is enabled");
            }
            context.getBundleContext().registerService(OAuthAdminServiceImpl.class.getName(), oauthAdminService, null);
            OauthInboundAuthConfigHandler authProtocolApplicationService = new OauthInboundAuthConfigHandler();
            OAuthComponentServiceHolder.getInstance().setOAuthInboundConfigHandler(
                    authProtocolApplicationService);
            context.getBundleContext().registerService(ApplicationInboundAuthConfigHandler.class,
                    authProtocolApplicationService, null);

            registerActionRequestBuilderAndResponseProcessor(context);
            registerRuleEvaluationDataProvider(context);
            // Note : DO NOT add any activation related code below this point,
            // to make sure the server doesn't start up if any activation failures occur

            if (log.isDebugEnabled()) {
                log.debug("Identity OAuth bundle is activated");
            }
        } catch (Throwable e) {
            String errMsg = "Error occurred while activating OAuth Service Component";
            log.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }

    private void registerActionRequestBuilderAndResponseProcessor(ComponentContext context) {

        context.getBundleContext()
                .registerService(ActionExecutionRequestBuilder.class, new PreIssueAccessTokenRequestBuilder(), null);
        context.getBundleContext()
                .registerService(ActionExecutionResponseProcessor.class, new PreIssueAccessTokenResponseProcessor(),
                        null);
    }

    private void registerRuleEvaluationDataProvider(ComponentContext context) {

        context.getBundleContext()
                .registerService(RuleEvaluationDataProvider.class, new PreIssueAccessTokenRuleEvaluationDataProvider(),
                        null);
    }

    protected void deactivate(ComponentContext context) {

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        if (log.isDebugEnabled()) {
            log.debug("Identity OAuth bundle is deactivated");
        }
    }

    @Reference(
            name = "realm.service",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        OAuthComponentServiceHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Realm Service");
        }
        OAuthComponentServiceHolder.getInstance().setRealmService(null);
    }

    @Reference(
            name = "scope.service",
            service = OAuth2ScopeService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOauth2ScopeService"
    )
    protected void setOauth2ScopeService(OAuth2ScopeService oauth2ScopeService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the Scope Service");
        }
        OAuthComponentServiceHolder.getInstance().setOauth2ScopeService(oauth2ScopeService);
    }

    protected void unsetOauth2ScopeService(OAuth2ScopeService oauth2ScopeService) {

        if (log.isDebugEnabled()) {
            log.debug("Un-setting the Scope Service");
        }
        OAuthComponentServiceHolder.getInstance().setOauth2ScopeService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.oauth.event.OAuthEventInterceptor",
            service = OAuthEventInterceptor.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOAuthEventInterceptor"
    )
    protected void setOAuthEventInterceptorProxy(OAuthEventInterceptor oAuthEventInterceptor) {

        if (oAuthEventInterceptor == null) {
            log.warn("Null Oauth Event Interceptor received, hence not registering");
            return;
        }

        if (!OAuthConstants.OAUTH_INTERCEPTOR_PROXY.equalsIgnoreCase(oAuthEventInterceptor.getName())) {
            log.debug("Non proxy Oauth event interceptor received, hence not registering");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Setting oauth event interceptor proxy :" + oAuthEventInterceptor.getClass().getName());
        }
        OAuthComponentServiceHolder.getInstance().addOauthEventInterceptorProxy(oAuthEventInterceptor);
    }

    protected void unsetOAuthEventInterceptor(OAuthEventInterceptor oAuthEventInterceptor) {

        if (oAuthEventInterceptor == null) {
            log.warn("Null oauth event interceptor received, hence not registering");
            return;
        }

        if (!OAuthConstants.OAUTH_INTERCEPTOR_PROXY.equalsIgnoreCase(oAuthEventInterceptor.getName())) {
            log.debug("Non proxy Oauth event interceptor received, hence not un-setting");
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Un-setting oauth event interceptor proxy :" + oAuthEventInterceptor.getClass().getName());
        }
        OAuthComponentServiceHolder.getInstance().addOauthEventInterceptorProxy(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.oauth2.token.handlers.response.AccessTokenResponseHandler",
            service = AccessTokenResponseHandler.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAccessTokenResponseHandler"
    )
    protected void setAccessTokenResponseHandler(AccessTokenResponseHandler accessTokenResponseHandler) {

        if (log.isDebugEnabled()) {
            log.debug("Access token response handler impl added: " + accessTokenResponseHandler.getClass().getName());
        }
        OAuthComponentServiceHolder.getInstance().addAccessTokenResponseHandler(accessTokenResponseHandler);
    }

    protected void unsetAccessTokenResponseHandler(AccessTokenResponseHandler accessTokenResponseHandler) {

        if (log.isDebugEnabled()) {
            log.debug("Access access token response handler removed: " +
                    accessTokenResponseHandler.getClass().getName());
        }
        OAuthComponentServiceHolder.getInstance().removeAccessTokenResponseHandler(accessTokenResponseHandler);
    }

    protected void unsetIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    @Reference(
            name = "identity.core.init.event.service",
            service = IdentityCoreInitializedEvent.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityCoreInitializedEventService"
    )
    protected void setIdentityCoreInitializedEventService(IdentityCoreInitializedEvent identityCoreInitializedEvent) {
        /* reference IdentityCoreInitializedEvent service to guarantee that this component will wait until identity core
         is started */
    }

    @Reference(name = "token.binding.service",
            service = TokenBinderInfo.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetTokenBinderInfo")
    protected void setTokenBinderInfo(TokenBinderInfo tokenBinderInfo) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the token binder info for: " + tokenBinderInfo.getBindingType());
        }
        OAuthComponentServiceHolder.getInstance().addTokenBinderInfo(tokenBinderInfo);
    }

    protected void unsetTokenBinderInfo(TokenBinderInfo tokenBinderInfo) {

        if (log.isDebugEnabled()) {
            log.debug("Un-setting the token binder info for: " + tokenBinderInfo.getBindingType());
        }
        OAuthComponentServiceHolder.getInstance().removeTokenBinderInfo(tokenBinderInfo);
    }

    @Reference(name = "oauth.application.mgt.listener",
            service = OAuthApplicationMgtListener.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOAuthApplicationMgtListener")
    protected void setOAuthApplicationMgtListener(OAuthApplicationMgtListener oAuthApplicationMgtListener) {

        if (log.isDebugEnabled()) {
            log.debug("Adding OAuthApplicationMgtListener: " + oAuthApplicationMgtListener.getClass().getName());
        }
        OAuthComponentServiceHolder.getInstance().addOAuthApplicationMgtListener(oAuthApplicationMgtListener);
    }

    protected void unsetOAuthApplicationMgtListener(OAuthApplicationMgtListener oAuthApplicationMgtListener) {

        if (log.isDebugEnabled()) {
            log.debug("Removing OAuthApplicationMgtListener: " + oAuthApplicationMgtListener.getClass().getName());
        }
        OAuthComponentServiceHolder.getInstance().removeOAuthApplicationMgtListener(oAuthApplicationMgtListener);
    }

    @Reference(
            name = "userSessionManagementService.service",
            service = UserSessionManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetUserSessionManagementService"
    )
    protected void setUserSessionManagementService(UserSessionManagementService userSessionManagementService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the User Session Management Service");
        }
        OAuth2ServiceComponentHolder.setUserSessionManagementService(userSessionManagementService);
    }

    protected void unsetUserSessionManagementService(UserSessionManagementService userSessionManagementService) {

        if (log.isDebugEnabled()) {
            log.debug("UnSetting the User Session Management Service");
        }
        OAuth2ServiceComponentHolder.setUserSessionManagementService(null);
    }

    @Reference(
            name = "RoleManagementServiceComponent",
            service = RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementService"
    )
    private void setRoleManagementService(RoleManagementService roleManagementService) {

        OAuthComponentServiceHolder.getInstance().setRoleManagementService(roleManagementService);
    }

    private void unsetRoleManagementService(RoleManagementService roleManagementService) {

        OAuthComponentServiceHolder.getInstance().setRoleManagementService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.role.v2.mgt.core.internal.RoleManagementServiceComponent",
            service = org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleV2ManagementService"
    )
    private void setRoleV2ManagementService(
            org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService roleV2ManagementService) {

        OAuthComponentServiceHolder.getInstance().setRoleV2ManagementService(roleV2ManagementService);
    }

    private void unsetRoleV2ManagementService(
            org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService roleV2ManagementService) {

        OAuthComponentServiceHolder.getInstance().setRoleV2ManagementService(null);
    }

    @Reference(
            name = "organization.user.resident.resolver.service",
            service = OrganizationUserResidentResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationUserResidentResolverService"
    )
    protected void setOrganizationUserResidentResolverService(
            OrganizationUserResidentResolverService organizationUserResidentResolverService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the organization user resident resolver service.");
        }
        OAuthComponentServiceHolder.getInstance().setOrganizationUserResidentResolverService(
                organizationUserResidentResolverService);
    }

    protected void unsetOrganizationUserResidentResolverService(
            OrganizationUserResidentResolverService organizationUserResidentResolverService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset organization user resident resolver service.");
        }
        OAuthComponentServiceHolder.getInstance().setOrganizationUserResidentResolverService(null);
    }

    @Reference(
            name = "organization.service",
            service = OrganizationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationManager"
    )
    protected void setOrganizationManager(OrganizationManager organizationManager) {

        OAuthComponentServiceHolder.getInstance().setOrganizationManager(organizationManager);
        log.debug("Set the organization management service.");
    }

    protected void unsetOrganizationManager(OrganizationManager organizationManager) {

        OAuthComponentServiceHolder.getInstance().setOrganizationManager(null);
        log.debug("Unset organization management service.");
    }

    @Reference(
            name = "application.mgt.service",
            service = ApplicationManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationManagementService"
    )
    protected void setApplicationManagementService(ApplicationManagementService applicationManagementService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting ApplicationManagement Service");
        }
        OAuthComponentServiceHolder.getInstance().
                setApplicationManagementService(applicationManagementService);
    }

    /**
     * Unsets ApplicationManagement Service.
     *
     * @param applicationManagementService An instance of ApplicationManagementService
     */
    protected void unsetApplicationManagementService(ApplicationManagementService applicationManagementService) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting ApplicationManagement.");
        }
        OAuthComponentServiceHolder.getInstance().setApplicationManagementService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.application.cors.mgt.service",
            service = CORSManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetApplicationCORSManagementService"
    )
    protected void setApplicationCORSManagementService(CORSManagementService corsManagementService) {

        OAuthComponentServiceHolder.getInstance().setCorsManagementService(corsManagementService);
    }

    protected void unsetApplicationCORSManagementService(CORSManagementService corsManagementService) {

        OAuthComponentServiceHolder.getInstance().setCorsManagementService(null);
    }

    @Reference(
            name = "identity.authorized.api.management.component",
            service = AuthorizedAPIManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAuthorizedAPIManagementService"
    )
    protected void setAuthorizedAPIManagementService(AuthorizedAPIManagementService authorizedAPIManagementService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting AuthorizedAPIManagementService Service");
        }
        OAuthComponentServiceHolder.getInstance().
                setAuthorizedAPIManagementService(authorizedAPIManagementService);
    }

    /**
     * Unsets AuthorizedAPIManagementService Service.
     *
     * @param authorizedAPIManagementService An instance of AuthorizedAPIManagementService
     */
    protected void unsetAuthorizedAPIManagementService(AuthorizedAPIManagementService authorizedAPIManagementService) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting AuthorizedAPIManagementService.");
        }
        OAuthComponentServiceHolder.getInstance().setAuthorizedAPIManagementService(null);
    }

    @Reference(
            name = "IdentityProviderManager",
            service = org.wso2.carbon.idp.mgt.IdpManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdpManager")
    protected void setIdpManager(IdpManager idpManager) {

        OAuthComponentServiceHolder.getInstance().setIdpManager(idpManager);
    }

    protected void unsetIdpManager(IdpManager idpManager) {

        OAuthComponentServiceHolder.getInstance().setIdpManager(null);
    }

    @Reference(
            name = "organization.user.sharing.service",
            service = OrganizationUserSharingService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrganizationUserSharingService"
    )
    protected void setOrganizationUserSharingService(OrganizationUserSharingService organizationUserSharingService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting the organization user sharing service.");
        }
        OAuthComponentServiceHolder.getInstance().setOrganizationUserSharingService(organizationUserSharingService);
    }

    protected void unsetOrganizationUserSharingService(OrganizationUserSharingService organizationUserSharingService) {

        if (log.isDebugEnabled()) {
            log.debug("Unset organization user sharing service.");
        }
        OAuthComponentServiceHolder.getInstance().setOrganizationUserSharingService(null);
    }

    /**
     * Set the ConfigurationManager.
     *
     * @param configurationManager The {@code ConfigurationManager} instance.
     */
    @Reference(
            name = "resource.configuration.manager",
            service = ConfigurationManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterConfigurationManager"
    )
    protected void registerConfigurationManager(ConfigurationManager configurationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Registering the ConfigurationManager in JWT Client Authenticator ManagementService.");
        }
        OAuthComponentServiceHolder.getInstance().setConfigurationManager(configurationManager);
    }

    /**
     * Unset the ConfigurationManager.
     *
     * @param configurationManager The {@code ConfigurationManager} instance.
     */
    protected void unregisterConfigurationManager(ConfigurationManager configurationManager) {

        if (log.isDebugEnabled()) {
            log.debug("Unregistering the ConfigurationManager in JWT Client Authenticator ManagementService.");
        }
        OAuthComponentServiceHolder.getInstance().setConfigurationManager(null);
    }

    @Reference(
            name = "action.execution.service",
            service = ActionExecutorService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterActionExecutorService"
    )
    protected void registerActionExecutionService(ActionExecutorService actionExecutorService) {

        log.debug("Registering the ActionExecutorService in OAuthServiceComponent.");
        OAuthComponentServiceHolder.getInstance().setActionExecutorService(actionExecutorService);
    }

    protected void unregisterActionExecutorService(ActionExecutorService actionExecutorService) {

        log.debug("Unregistering the ActionExecutorService in OAuthServiceComponent.");
        OAuthComponentServiceHolder.getInstance().setActionExecutorService(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.organization.resource.hierarchy.traverse.service",
            service = OrgResourceResolverService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetOrgResourceResolverService")
    protected void setOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        OAuthComponentServiceHolder.getInstance()
                .setOrgResourceResolverService(orgResourceResolverService);
        log.debug("OrgResourceResolverService set in Claim Management bundle");
    }

    protected void unsetOrgResourceResolverService(OrgResourceResolverService orgResourceResolverService) {

        OAuthComponentServiceHolder.getInstance().setOrgResourceResolverService(null);
        log.debug("OrgResourceResolverService unset in Claim Management bundle");
    }
}
