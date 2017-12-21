/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.account.suspension.notification.task.internal;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.account.suspension.notification.task.NotificationReceiversRetrievalFactory;
import org.wso2.carbon.identity.account.suspension.notification.task.handler.AccountSuspensionNotificationHandler;
import org.wso2.carbon.identity.account.suspension.notification.task.jdbc.JDBCNotificationReceiversRetrievalFactory;
import org.wso2.carbon.identity.account.suspension.notification.task.ldap.LDAPNotificationReceiversRetrievalFactory;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.event.services.IdentityEventService;
import org.wso2.carbon.identity.governance.IdentityGovernanceService;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;

@Component(
        name = "NotificationTaskServiceComponent",
        immediate = true)
public class NotificationTaskServiceComponent {

    /*

    * */
    private static final Logger log = Logger.getLogger(org.wso2.carbon.identity.account.suspension.notification.task.internal.NotificationTaskServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) throws UserStoreException {
        BundleContext bundleContext = context.getBundleContext();
        NotificationTaskDataHolder.getInstance().setBundleContext(bundleContext);
        AccountSuspensionNotificationHandler handler = new AccountSuspensionNotificationHandler();
        context.getBundleContext().registerService(AbstractEventHandler.class.getName(), handler, null);
        LDAPNotificationReceiversRetrievalFactory ladLdapNotificationReceiversRetrievalFactory = new LDAPNotificationReceiversRetrievalFactory();
        bundleContext.registerService(NotificationReceiversRetrievalFactory.class.getName(), ladLdapNotificationReceiversRetrievalFactory, null);
        JDBCNotificationReceiversRetrievalFactory jdbcNotificationReceiversRetrievalFactory = new JDBCNotificationReceiversRetrievalFactory();
        bundleContext.registerService(NotificationReceiversRetrievalFactory.class.getName(), jdbcNotificationReceiversRetrievalFactory, null);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Notification bundle de-activated");
        }
    }

    protected void unsetIdentityEventService(IdentityEventService eventService) {
        NotificationTaskDataHolder.getInstance().setIdentityEventService(null);
    }

    @Reference(
            name = "EventMgtService",
            service = org.wso2.carbon.identity.event.services.IdentityEventService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityEventService")
    protected void setIdentityEventService(IdentityEventService eventService) {
        NotificationTaskDataHolder.getInstance().setIdentityEventService(eventService);
    }

    protected void unsetIdentityGovernanceService(IdentityGovernanceService idpManager) {
        NotificationTaskDataHolder.getInstance().setIdentityGovernanceService(null);
    }

    @Reference(
            name = "IdentityGovernanceService",
            service = org.wso2.carbon.identity.governance.IdentityGovernanceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetIdentityGovernanceService")
    protected void setIdentityGovernanceService(IdentityGovernanceService idpManager) {
        NotificationTaskDataHolder.getInstance().setIdentityGovernanceService(idpManager);
    }

    @Reference(
            name = "NotificationTaskServiceComponent",
            service = org.wso2.carbon.identity.account.suspension.notification.task.NotificationReceiversRetrievalFactory.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetNotificationReceiversRetrievalFactory")
    protected void setNotificationReceiversRetrievalFactory(NotificationReceiversRetrievalFactory notificationReceiversRetrievalFactory) {
        NotificationTaskDataHolder.getInstance().getNotificationReceiversRetrievalFactories().put(notificationReceiversRetrievalFactory.getType(), notificationReceiversRetrievalFactory);
        if (log.isDebugEnabled()) {
            log.debug("Added notification retriever : " + notificationReceiversRetrievalFactory.getType());
        }
    }

    protected void unsetNotificationReceiversRetrievalFactory(NotificationReceiversRetrievalFactory notificationReceiversRetrievalFactory) {
        NotificationTaskDataHolder.getInstance().getNotificationReceiversRetrievalFactories().remove(notificationReceiversRetrievalFactory.getType());
        if (log.isDebugEnabled()) {
            log.debug("Removed notification retriever : " + notificationReceiversRetrievalFactory.getType());
        }
    }

    @Reference(
            name = "user.realmservice.default",
            service = org.wso2.carbon.user.core.service.RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService")
    protected void setRealmService(RealmService realmService) {
        NotificationTaskDataHolder.getInstance().setRealmService(realmService);
        if (log.isDebugEnabled()) {
            log.debug("RealmService is set in the User Store Count bundle");
        }
    }

    protected void unsetRealmService(RealmService realmService) {
        NotificationTaskDataHolder.getInstance().setRealmService(null);
        if (log.isDebugEnabled()) {
            log.debug("RealmService is unset in the Application Authentication Framework bundle");
        }
    }
}

