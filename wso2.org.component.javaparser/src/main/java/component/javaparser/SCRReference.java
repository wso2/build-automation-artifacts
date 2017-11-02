package component.javaparser;

public class SCRReference {

    private String referenceName;
    private String service;
    private String cardinality;
    private String policy;
    private String unbind;
    private String bind;

    public SCRReference() {
    }

    public String getBind() {
        return bind;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void setReferenceName(String referenceName) {
        this.referenceName = referenceName;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getCardinality() {
        return cardinality;
    }

    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getUnbind() {
        return unbind;
    }

    public void setUnbind(String unbind) {
        this.unbind = unbind;
    }

    private String cardinalityMapping(String cardinality) {
        if (cardinality.equals("0..1")) {
            cardinality = "ReferenceCardinality.OPTIONAL";
        } else if (cardinality.equals("1..1")) {
            cardinality = "ReferenceCardinality.MANDATORY";
        } else if (cardinality.equals("0..n")) {
            cardinality = "ReferenceCardinality.MULTIPLE";
        } else if (cardinality.equals("1..n")) {
            cardinality = "ReferenceCardinality.AT_LEAST_ONE";
        } else {
            return null;
        }
        return cardinality;
    }

    public String getNewCardinality() {
        String newCardinality = cardinalityMapping(cardinality);
        return newCardinality;
    }

    private String policyMapping(String policy) {
        if (policy.equals("dynamic")) {
            policy = "ReferencePolicy.DYNAMIC";
        } else if (policy.equals("static")) {
            policy = "ReferencePolicy.STATIC";
        } else {
            return null;
        }
        return policy;
    }

    public String getNewPolicy() {
        String newPolicy = policyMapping(policy);
        return newPolicy;
    }

    public String getNewService() {
        String newService = service + ".class";
        return newService;
    }

}
