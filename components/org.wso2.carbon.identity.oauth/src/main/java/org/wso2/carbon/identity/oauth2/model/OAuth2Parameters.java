/*
 * Copyright (c) 2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.oauth2.model;

import org.wso2.carbon.identity.oauth.rar.model.AuthorizationDetails;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A Bean class which is used to store the OAuth parameters available in a OAuth request in the Cache.
 */
public class OAuth2Parameters implements Serializable {

    private static final long serialVersionUID = 2237345658556955974L;

    private String applicationName;
    private String redirectURI;
    private Set<String> scopes;
    private Set<String> consentRequiredScopes;
    private String state;
    private String responseType;
    private String clientId;
    private String nonce;
    private String display;
    private String prompt;
    private String idTokenHint;
    private String loginHint;
    private String pkceCodeChallenge;
    private String pkceCodeChallengeMethod;
    private LinkedHashSet acrValues;
    private String tenantDomain;
    private long authTime;
    private long maxAge;
    private String responseMode;
    private String essentialClaims;
    private String displayName;
    private String sessionDataKey;
    private String loginTenantDomain;
    private boolean isRequestObjectFlow;
    private boolean isMtlsRequest;
    private String requestedSubjectId;
    private AuthorizationDetails authorizationDetails;
    private String requestedActor;

    public String getRequestedSubjectId() {

        return requestedSubjectId;
    }

    public void setRequestedSubjectId(String requestedSubjectId) {

        this.requestedSubjectId = requestedSubjectId;
    }

    public String getSessionDataKey() {
        return sessionDataKey;
    }

    public void setSessionDataKey(String sessionDataKey) {
        this.sessionDataKey = sessionDataKey;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public String getEssentialClaims() {
        return essentialClaims;
    }

    public void setEssentialClaims(String essentialClaims) {
        this.essentialClaims = essentialClaims;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public long getAuthTime() {
        return authTime;
    }

    public void setAuthTime(long authTime) {
        this.authTime = authTime;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRedirectURI() {
        return redirectURI;
    }

    public void setRedirectURI(String redirectURI) {
        this.redirectURI = redirectURI;
    }

    public Set<String> getConsentRequiredScopes() {

        return consentRequiredScopes;
    }

    public void setConsentRequiredScopes(Set<String> consentRequiredScopes) {

        this.consentRequiredScopes = consentRequiredScopes;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the nonce
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * @param nonce the nonce to set
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * @return the display
     */
    public String getDisplay() {
        return display;
    }

    /**
     * @param display the display to set
     */
    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * @return the idTokenHint
     */
    public String getIDTokenHint() {
        return idTokenHint;
    }

    /**
     * @param idTokenHint the idTokenHint to set
     */
    public void setIDTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    /**
     * @return the loginHint
     */
    public String getLoginHint() {
        return loginHint;
    }

    /**
     * @param loginHint the loginHint to set
     */
    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public LinkedHashSet getACRValues() {
        return acrValues;
    }

    public void setACRValues(LinkedHashSet acrValues) {
        this.acrValues = acrValues;
    }

    public String getPkceCodeChallenge() {
        return pkceCodeChallenge;
    }

    public void setPkceCodeChallenge(String pkceCodeChallenge) {
        this.pkceCodeChallenge = pkceCodeChallenge;
    }

    public String getPkceCodeChallengeMethod() {
        return pkceCodeChallengeMethod;
    }

    public void setPkceCodeChallengeMethod(String pkceCodeChallengeMethod) {
        this.pkceCodeChallengeMethod = pkceCodeChallengeMethod;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the tenant domain the user's session should be created.
     */
    public String getLoginTenantDomain() {

        return loginTenantDomain;
    }

    /**
     * Sets the tenant domain where the user's session should be created.
     *
     * @param loginTenantDomain the tenant domain where the user's session is created.
     */
    public void setLoginTenantDomain(String loginTenantDomain) {

        this.loginTenantDomain = loginTenantDomain;
    }

    /**
     *
     * @return  Whether the flow has request object or not.
     */
    public boolean isRequestObjectFlow() {

        return isRequestObjectFlow;
    }

    /**
     * Sets whether the the flow has request object or not.
     *
     * @param isRequestObjectFlow   Is flow has request object or not.
     */
    public void setRequestObjectFlow(boolean isRequestObjectFlow) {

        this.isRequestObjectFlow = isRequestObjectFlow;
    }

    /**
     * @return  Whether the request is a mutual TLS request or not.
     */
    public boolean isMtlsRequest() {

        return isMtlsRequest;
    }

    /**
     * Sets whether the request is a mutual TLS request or not.
     *
     * @param isMtlsRequest Whether the request is a mutual TLS request or not.
     */
    public void setIsMtlsRequest(boolean isMtlsRequest) {

        this.isMtlsRequest = isMtlsRequest;
    }

    /**
     * Retrieves the current authorization details.
     *
     * @return the {@link AuthorizationDetails} instance representing the current authorization information.
     * If no authorization details are available, it will return {@code null}.
     */
    public AuthorizationDetails getAuthorizationDetails() {

        return this.authorizationDetails;
    }

    /**
     * Sets the authorization details.
     * This method updates the authorization details with the provided {@link AuthorizationDetails} instance.
     *
     * @param authorizationDetails the {@link AuthorizationDetails} to set.
     */
    public void setAuthorizationDetails(final AuthorizationDetails authorizationDetails) {

        this.authorizationDetails = authorizationDetails;
    }

    public String getRequestedActor() {

        return requestedActor;
    }

    public void setRequestedActor(String requestedActor) {

        this.requestedActor = requestedActor;
    }
}
