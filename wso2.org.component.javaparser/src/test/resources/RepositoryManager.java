/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.sample.repository.mgt;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.kernel.startupresolver.StartupServiceUtils;
import org.wso2.carbon.sample.transport.mgt.Transport;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample Transport Manager Service Component class.
 *
 * @since 5.0.0
 */
@Component(
        name = "org.wso2.carbon.sample.repository.mgt.RepositoryManager",
        immediate = true
)
public class RepositoryManager {

    private static List<Transport> transportList = new ArrayList<>();

    private static List<Runtime> runtimeList = new ArrayList<>();

    @Activate
    public void activate(BundleContext bundleContext) {
    }

    @Deactivate
    public void deactivate(BundleContext bundleContext) {

    }

    @Reference(
            name = "sample.transport.service.reference",
            service = Transport.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "deregisterTransport"
    )
    public void registerTransport(Transport transport) {
        transportList.add(transport);
        StartupServiceUtils.updateServiceCache(RepositoryServicesListener.COMPONENT_NAME, Transport.class);
    }

    public void deregisterTransport(Transport transport) {
        transportList.remove(transport);
    }

    public int getTransportCount() {
        return transportList.size();
    }

    @Reference(
            name = "sample.runtime.service.reference",
            service = Runtime.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "deregisterRuntime"
    )
    public void registerRuntime(Runtime runtime) {
        runtimeList.add(runtime);
        StartupServiceUtils.updateServiceCache(RepositoryServicesListener.COMPONENT_NAME, Runtime.class);
    }

    public void deregisterRuntime(Runtime runtime) {
        runtimeList.remove(runtime);
    }

    public int getRuntimeCount() {
        return runtimeList.size();
    }

}
