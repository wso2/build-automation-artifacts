/*
 * Copyright (c) 2005, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.carbon.identity.sso.saml.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.ContextPathServletAdaptor;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.identity.base.IdentityConstants;
import org.wso2.carbon.identity.core.util.IdentityIOStreamUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.sso.saml.SAMLSSOConstants;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.carbon.identity.sso.saml.admin.FileBasedConfigManager;
import org.wso2.carbon.identity.sso.saml.servlet.SAMLSSOProviderServlet;
import org.wso2.carbon.identity.sso.saml.util.SAMLSSOUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import javax.servlet.Servlet;

/**
 * @scr.component name="identity.sso.saml.component" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="osgi.httpservice" interface="org.osgi.service.http.HttpService"
 * cardinality="1..1" policy="dynamic" bind="setHttpService"
 * unbind="unsetHttpService"
 */
public class IdentitySAMLSSOServiceComponent {

    private static Log log = LogFactory.getLog(org.wso2.carbon.identity.sso.saml.internal.IdentitySAMLSSOServiceComponent.class);
    private static int defaultSingleLogoutRetryCount = 5;

    private static ServerConfigurationService serverConfigurationService = null;
    private static long defaultSingleLogoutRetryInterval = 60000;

    private static String ssoRedirectPage = null;

    public static String getSsoRedirectHtml() {
        return ssoRedirectPage;
    }

    protected void activate(ComponentContext ctxt) {
        SAMLSSOUtil.setBundleContext(ctxt.getBundleContext());
        HttpService httpService = SAMLSSOUtil.getHttpService();

        // Register SAML SSO servlet
        Servlet samlSSOServlet = new ContextPathServletAdaptor(new SAMLSSOProviderServlet(),
                                                               SAMLSSOConstants.SAMLSSO_URL);
        try {
            httpService.registerServlet(SAMLSSOConstants.SAMLSSO_URL, samlSSOServlet, null, null);
        } catch (Exception e) {
            String errMsg = "Error when registering SAML SSO Servlet via the HttpService.";
            log.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }

        // Register a SSOServiceProviderConfigManager object as an OSGi Service
        ctxt.getBundleContext().registerService(SSOServiceProviderConfigManager.class.getName(),
                SSOServiceProviderConfigManager.getInstance(), null);
        String redirectHtmlPath = null;
        FileInputStream fis = null;
        try {
            IdentityUtil.populateProperties();
            SAMLSSOUtil.setSingleLogoutRetryCount(Integer.parseInt(
                    IdentityUtil.getProperty(IdentityConstants.ServerConfig.SINGLE_LOGOUT_RETRY_COUNT)));
            SAMLSSOUtil.setSingleLogoutRetryInterval(Long.parseLong(IdentityUtil.getProperty(
                    IdentityConstants.ServerConfig.SINGLE_LOGOUT_RETRY_INTERVAL)));

            SAMLSSOUtil.setResponseBuilder(IdentityUtil.getProperty("SSOService.SAMLSSOResponseBuilder"));
            SAMLSSOUtil.setIdPInitSSOAuthnRequestValidator(IdentityUtil.getProperty("SSOService.IdPInitSSOAuthnRequestValidator"));
            SAMLSSOUtil.setSPInitSSOAuthnRequestProcessor(IdentityUtil.getProperty("SSOService.SPInitSSOAuthnRequestProcessor"));
            SAMLSSOUtil.setSPInitLogoutRequestProcessor(IdentityUtil.getProperty("SSOService.SPInitSSOAuthnRequestProcessor"));
            SAMLSSOUtil.setIdPInitLogoutRequestProcessor(IdentityUtil.getProperty("SSOService.IdPInitLogoutRequestProcessor"));
            SAMLSSOUtil.setIdPInitSSOAuthnRequestProcessor(IdentityUtil.getProperty("SSOService.IdPInitSSOAuthnRequestProcessor"));

            if (log.isDebugEnabled()) {
                log.debug("IdPInitSSOAuthnRequestValidator is set to " +
                        IdentityUtil.getProperty("SSOService.IdPInitSSOAuthnRequestValidator"));
                log.debug("SPInitSSOAuthnRequestValidator is set to " +
                        IdentityUtil.getProperty("SSOService.SPInitSSOAuthnRequestValidator"));
                log.debug("SPInitSSOAuthnRequestProcessor is set to " +
                        IdentityUtil.getProperty("SSOService.SPInitSSOAuthnRequestProcessor"));
                log.debug("SPInitLogoutRequestProcessor is set to " +
                        IdentityUtil.getProperty("SSOService.SPInitLogoutRequestProcessor"));
                log.debug("IdPInitLogoutRequestProcessor is set to " +
                        IdentityUtil.getProperty("SSOService.IdPInitLogoutRequestProcessor"));
                log.debug("IdPInitSSOAuthnRequestProcessor is set to " +
                        IdentityUtil.getProperty("SSOService.IdPInitSSOAuthnRequestProcessor"));
                log.debug("Single logout retry count is set to " + SAMLSSOUtil.getSingleLogoutRetryCount());
                log.debug("Single logout retry interval is set to " +
                        SAMLSSOUtil.getSingleLogoutRetryInterval() + " in seconds.");
            }

            redirectHtmlPath = CarbonUtils.getCarbonHome() + File.separator + "repository"
                    + File.separator + "resources" + File.separator + "identity" + File.separator + "pages" + File.separator + "samlsso_response.html";
            fis = new FileInputStream(new File(redirectHtmlPath));
            ssoRedirectPage = new Scanner(fis, "UTF-8").useDelimiter("\\A").next();
            if (log.isDebugEnabled()) {
                log.debug("samlsso_response.html " + ssoRedirectPage);
            }

            FileBasedConfigManager.getInstance().addServiceProviders();

            if (log.isDebugEnabled()) {
                log.debug("Identity SAML SSO bundle is activated");
            }
        } catch (FileNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to find SAML SSO response page in : " + redirectHtmlPath);
            }
        } catch (Throwable e) {
            SAMLSSOUtil.setSingleLogoutRetryCount(defaultSingleLogoutRetryCount);
            SAMLSSOUtil.setSingleLogoutRetryInterval(defaultSingleLogoutRetryInterval);
            if (log.isDebugEnabled()) {
                log.debug("Failed to load the single logout retry count and interval values." +
                        " Default values for retry count: " + defaultSingleLogoutRetryCount +
                        " and interval: " + defaultSingleLogoutRetryInterval + " will be used.", e);
            }
        } finally {
            IdentityIOStreamUtils.closeInputStream(fis);
        }

    }

    protected void deactivate(ComponentContext ctxt) {
        SAMLSSOUtil.setBundleContext(null);
        if (log.isDebugEnabled()) {
            log.info("Identity SAML SSO bundle is deactivated");
        }
    }

    /**
     * This method is used to get created realm service
     *
     * @return RealmService RealmService instance to connect with user store
     */
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService set in Identity SAML SSO bundle");
        }
        try {
            SAMLSSOUtil.setRegistryService(registryService);
        } catch (Throwable e) {
            log.error("Failed to get a reference to the Registry in SAML SSO bundle", e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unset in SAML SSO bundle");
        }
        SAMLSSOUtil.setRegistryService(null);
    }
    /**
     * This method is used to set realm service
     *
     * @param realmService <code>RealmService</code>
     */
    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Realm Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Realm Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setRegistryService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService configCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("Configuration Context Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setConfigCtxService(configCtxService);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configCtxService) {
        if (log.isDebugEnabled()) {
            log.debug("Configuration Context Service is unset in the SAML SSO bundle");
        }
        SAMLSSOUtil.setConfigCtxService(null);
    }

    protected void setHttpService(HttpService httpService) {
        if (log.isDebugEnabled()) {
            log.debug("HTTP Service is set in the SAML SSO bundle");
        }
        SAMLSSOUtil.setHttpService(httpService);
    }

    protected void unsetHttpService(HttpService httpService) {
        if (log.isDebugEnabled()) {
            log.debug("HTTP Service is unset in the SAML SSO bundle");
        }
        SAMLSSOUtil.setHttpService(null);
    }

    public static ServerConfigurationService getServerConfigurationService() {
        return org.wso2.carbon.identity.sso.saml.internal.IdentitySAMLSSOServiceComponent.serverConfigurationService;
    }

    protected void setServerConfigurationService(ServerConfigurationService serverConfigurationService) {
        if (log.isDebugEnabled()) {
            log.debug("Set the ServerConfiguration Service");
        }
        org.wso2.carbon.identity.sso.saml.internal.IdentitySAMLSSOServiceComponent.serverConfigurationService = serverConfigurationService;

    }
    protected void unsetServerConfigurationService(ServerConfigurationService serverConfigurationService) {
        if (log.isDebugEnabled()) {
            log.debug("Unset the ServerConfiguration Service");
        }
        org.wso2.carbon.identity.sso.saml.internal.IdentitySAMLSSOServiceComponent.serverConfigurationService = null;
    }
}