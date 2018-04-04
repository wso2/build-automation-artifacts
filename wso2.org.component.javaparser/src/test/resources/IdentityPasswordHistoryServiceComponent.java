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
package org.wso2.carbon.identity.password.history.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.identity.password.history.handler.PasswordHistoryValidationHandler;

@Component(
        name = "org.wso2.carbon.identity.recovery.internal.IdentityRecoveryServiceComponent",
        immediate = true)
public class IdentityPasswordHistoryServiceComponent {

    private static Log log = LogFactory.getLog(IdentityPasswordHistoryServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Identity Management Listener is enabled");
            }
            BundleContext bundleContext = context.getBundleContext();
            IdentityPasswordHistoryServiceDataHolder.getInstance().setBundleContext(bundleContext);
            PasswordHistoryValidationHandler handler = new PasswordHistoryValidationHandler();
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(), handler, null);
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

    protected void unsetIdentityGovernanceService(IdentityGovernanceService idpManager) {
        IdentityPasswordHistoryServiceDataHolder.getInstance().setIdentityGovernanceService(null);
    }

    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService idpManager) {
        IdentityPasswordHistoryServiceDataHolder.getInstance().setIdentityGovernanceService(idpManager);
    }
}

