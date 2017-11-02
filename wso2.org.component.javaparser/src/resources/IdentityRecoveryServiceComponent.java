/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.identity.recovery.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.governance.common.IdentityConnectorConfig;
import org.wso2.carbon.identity.recovery.ChallengeQuestionManager;
import org.wso2.carbon.identity.recovery.IdentityRecoveryException;
import org.wso2.carbon.identity.recovery.connector.AdminForcedPasswordResetConfigImpl;
import org.wso2.carbon.identity.recovery.connector.RecoveryConfigImpl;
import org.wso2.carbon.identity.recovery.connector.SelfRegistrationConfigImpl;
import org.wso2.carbon.identity.recovery.connector.UserEmailVerificationConfigImpl;
import org.wso2.carbon.identity.recovery.handler.AccountConfirmationValidationHandler;
import org.wso2.carbon.identity.recovery.handler.AdminForcedPasswordResetHandler;
import org.wso2.carbon.identity.recovery.handler.UserEmailVerificationHandler;
import org.wso2.carbon.identity.recovery.handler.UserSelfRegistrationHandler;
import org.wso2.carbon.identity.recovery.listener.TenantManagementListener;
import org.wso2.carbon.identity.recovery.password.NotificationPasswordRecoveryManager;
import org.wso2.carbon.identity.recovery.password.SecurityQuestionPasswordRecoveryManager;
import org.wso2.carbon.identity.recovery.signup.UserSelfRegistrationManager;
import org.wso2.carbon.identity.recovery.username.NotificationUsernameRecoveryManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

@Reference(
        name = "org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceComponent",
        immediate = true)
public class IdentityRecoveryServiceComponent {

    private static Log log = LogFactory.getLog(IdentityRecoveryServiceComponent.class);

    private IdentityRecoveryServiceDataHolder dataHolder = IdentityRecoveryServiceDataHolder.getInstance();

    @Activate
    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(NotificationPasswordRecoveryManager.class.getName(), NotificationPasswordRecoveryManager.getInstance(), null);
            bundleContext.registerService(SecurityQuestionPasswordRecoveryManager.class.getName(), SecurityQuestionPasswordRecoveryManager.getInstance(), null);
            bundleContext.registerService(NotificationUsernameRecoveryManager.class.getName(), NotificationUsernameRecoveryManager.getInstance(), null);
            bundleContext.registerService(UserSelfRegistrationManager.class.getName(), UserSelfRegistrationManager.getInstance(), null);
            bundleContext.registerService(ChallengeQuestionManager.class.getName(), ChallengeQuestionManager.getInstance(), null);
            bundleContext.registerService(AbstractEventHandler.class.getName(), new AccountConfirmationValidationHandler(), null);
            bundleContext.registerService(AbstractEventHandler.class.getName(), new UserSelfRegistrationHandler(), null);
            bundleContext.registerService(AbstractEventHandler.class.getName(), new UserEmailVerificationHandler(), null);
            bundleContext.registerService(AbstractEventHandler.class.getName(), new AdminForcedPasswordResetHandler(), null);
            bundleContext.registerService(IdentityConnectorConfig.class.getName(), new RecoveryConfigImpl(), null);
            bundleContext.registerService(IdentityConnectorConfig.class.getName(), new SelfRegistrationConfigImpl(), null);
            bundleContext.registerService(IdentityConnectorConfig.class.getName(), new UserEmailVerificationConfigImpl(), null);
            bundleContext.registerService(IdentityConnectorConfig.class.getName(), new AdminForcedPasswordResetConfigImpl(), null);
        } catch (Exception e) {
            log.error("Error while activating identity governance component.", e);
        }
        // register the tenant management listener
        TenantMgtListener tenantMgtListener = new TenantManagementListener();
        context.getBundleContext().registerService(TenantMgtListener.class.getName(), tenantMgtListener, null);
        // register default challenge questions
        try {
            if (log.isDebugEnabled()) {
                log.debug("Loading default challenge questions for super tenant.");
            }
            loadDefaultChallengeQuestions();
            //   new ChallengeQuestionManager().getAllChallengeQuestions("carbon.super", "lk_LK");
        } catch (IdentityRecoveryException e) {
            log.error("Error persisting challenge question for super tenant.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Identity Management bundle is de-activated");
        }
    }

    @Reference(
            name = "realm.service",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        dataHolder.setRealmService(realmService);
    }

    @Reference(
            name = "registry.service",
            service = org.wso2.carbon.registry.core.service.RegistryService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRegistryService")
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }
        dataHolder.setRegistryService(registryService);
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        dataHolder.setRealmService(null);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        log.debug("UnSetting the Registry Service");
        dataHolder.setRegistryService(null);
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {
        IdentityRecoveryServiceDataHolder.getInstance().setIdentityEventService(null);
    }

    @Reference(
            name = "IdentityEventService",
            service = org.wso2.carbon.identity.event.services.IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService")
    protected void setIdentityEventService(IdentityEventService identityEventService) {
        IdentityRecoveryServiceDataHolder.getInstance().setIdentityEventService(identityEventService);
    }

    protected void unsetIdentityGovernanceService(IdentityGovernanceService idpManager) {
        dataHolder.setIdentityGovernanceService(null);
    }

    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService idpManager) {
        dataHolder.setIdentityGovernanceService(idpManager);
    }

    protected void unsetResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        dataHolder.setResourceMgtService(null);
        if (log.isDebugEnabled()) {
            log.debug("Setting Identity Resource Mgt service.");
        }
    }

    @Reference(
            name = "RegistryResourceMgtService",
            service = org.wso2.carbon.identity.core.persistence.registry.RegistryResourceMgtService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetResourceMgtService")
    protected void setResourceMgtService(RegistryResourceMgtService registryResourceMgtService) {
        dataHolder.setResourceMgtService(registryResourceMgtService);
        if (log.isDebugEnabled()) {
            log.debug("Unsetting Identity Resource Mgt service.");
        }
    }

    private void loadDefaultChallengeQuestions() throws IdentityRecoveryException {
        String tenantDomain = MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
        ChallengeQuestionManager.getInstance().setDefaultChallengeQuestions(tenantDomain);
    }
}
