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
package org.wso2.carbon.identity.governance.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.governance.IdentityGovernanceServiceImpl;
import org.wso2.carbon.identity.governance.IdentityGovernanceUtil;
import org.wso2.carbon.identity.governance.common.IdentityConnectorConfig;
import org.wso2.carbon.identity.governance.listener.IdentityMgtEventListener;
import org.wso2.carbon.identity.governance.listener.IdentityStoreEventListener;
import org.wso2.carbon.identity.governance.listener.TenantCreationEventListener;
import org.wso2.carbon.idp.mgt.IdpManager;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

@Component(
        name = "org.wso2.carbon.identity.governance.internal.IdentityMgtServiceComponent",
        immediate = true)
public class IdentityMgtServiceComponent {

    private static Log log = LogFactory.getLog(IdentityMgtServiceComponent.class);

    private static IdentityMgtEventListener listener = null;

    @Activate
    protected void activate(ComponentContext context) {
        try {
            listener = new IdentityMgtEventListener();
            context.getBundleContext().registerService(UserOperationEventListener.class, listener, null);
            context.getBundleContext().registerService(UserOperationEventListener.class, new IdentityStoreEventListener(), null);
            context.getBundleContext().registerService(IdentityGovernanceService.class, new IdentityGovernanceServiceImpl(), null);
            context.getBundleContext().registerService(TenantMgtListener.class.getName(), new TenantCreationEventListener(), null);
            if (log.isDebugEnabled()) {
                log.debug("Identity Management Listener is enabled");
            }
        } catch (Exception e) {
            log.error("Error while activating identity governance component.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Identity Management bundle is de-activated");
        }
    }

    protected void unsetIdentityEventService(IdentityEventService identityEventService) {
        IdentityMgtServiceDataHolder.getInstance().setIdentityEventService(null);
    }

    @Reference(
            name = "EventMgtService",
            service = org.wso2.carbon.identity.event.services.IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService")
    protected void setIdentityEventService(IdentityEventService identityEventService) {
        IdentityMgtServiceDataHolder.getInstance().setIdentityEventService(identityEventService);
    }

    protected void unsetIdentityGovernanceConnector(IdentityConnectorConfig identityConnectorConfig) {
        IdentityMgtServiceDataHolder.getInstance().unsetIdentityGovernanceConnector(identityConnectorConfig);
    }

    @Reference(
            name = "idp.mgt.event.listener.service",
            service = org.wso2.carbon.identity.governance.common.IdentityConnectorConfig.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceConnector")
    protected void setIdentityGovernanceConnector(IdentityConnectorConfig identityConnectorConfig) {
        IdentityMgtServiceDataHolder.getInstance().addIdentityGovernanceConnector(identityConnectorConfig);
        try {
            IdentityGovernanceUtil.saveConnectorDefaultProperties(identityConnectorConfig, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        } catch (IdentityGovernanceException e) {
            log.error("Error while saving super tenant configurations for " + identityConnectorConfig.getName() + ".", e);
        }
    }

    protected void unsetIdpManager(IdpManager idpManager) {
        IdentityMgtServiceDataHolder.getInstance().setIdpManager(null);
    }

    @Reference(
            name = "IdentityProviderManager",
            service = org.wso2.carbon.idp.mgt.IdpManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdpManager")
    protected void setIdpManager(IdpManager idpManager) {
        IdentityMgtServiceDataHolder.getInstance().setIdpManager(idpManager);
    }

    @Reference(
            name = "RealmService",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Realm Service");
        }
        IdentityMgtServiceDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        IdentityMgtServiceDataHolder.getInstance().setRealmService(null);
    }
}

