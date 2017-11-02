package component.javaparser;

import java.util.ArrayList;
import java.util.List;

public class SCRComponent {

    private String componentName;
    private String immediate;
    private String clazz;
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
