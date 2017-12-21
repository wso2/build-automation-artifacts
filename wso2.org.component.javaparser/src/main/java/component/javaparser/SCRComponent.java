/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package component.javaparser;

import java.util.ArrayList;
import java.util.List;

/**
 * This class has been use for the create  @Component annotation object.
 */
public class SCRComponent {

    private String componentName;
    private String immediate;
    private List<SCRReference> scrReferences = new ArrayList<>();

    public SCRComponent(String componentName, String immediate) {
        this.componentName = componentName;
        this.immediate = immediate;
    }

    public void addReferences(SCRReference scrReference) {
        scrReferences.add(scrReference);
    }

    public List<SCRReference> getScrReferences() {
        return this.scrReferences;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getImmediateName() {
        return immediate;
    }

}
