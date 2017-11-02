package component.javaparser;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

public class MethodVisitor extends VoidVisitorAdapter<Void> {

    private SCRComponent scrComponent;

    /**
     * this method will be called for all methods in this CompilationUnit, including inner class methods
     *
     * @param n
     * @param arg
     */
    @Override
    public void visit(MethodDeclaration n, Void arg) {
        addAnnotation(n);
        super.visit(n, arg);
    }

    private NormalAnnotationExpr addReferenceAnnotation(SCRReference scrReference) {
        NodeList<MemberValuePair> nodeList = new NodeList<>();

        NormalAnnotationExpr normalAnnotationExpr = new NormalAnnotationExpr(new Name("Reference"), nodeList);
        char c = '"';
        normalAnnotationExpr.addPair("\n             name", c + scrReference.getReferenceName() + c);
        normalAnnotationExpr.addPair("\n             service", scrReference.getNewService());
        normalAnnotationExpr.addPair("\n             cardinality", scrReference.getNewCardinality());
        normalAnnotationExpr.addPair("\n             policy", scrReference.getNewPolicy());
        normalAnnotationExpr.addPair("\n             unbind", c + scrReference.getUnbind() + c);

        return normalAnnotationExpr;
    }

    public void setScrComponent(SCRComponent scrComponent) {
        this.scrComponent = scrComponent;
    }

    private void addAnnotation(MethodDeclaration n) {
        List<SCRReference> scrReference = scrComponent.getScrReferences();
        if (n.getName().asString().equals("activate")) {
            n.addMarkerAnnotation("Activate");
        } else if (n.getName().asString().equals("deactivate")) {
            n.addMarkerAnnotation("Deactivate");
        }
        for (int i = 0; i < scrReference.size(); i++) {
            if (n.getName().asString().equals(scrReference.get(i).getBind())) {
                n.addAnnotation(addReferenceAnnotation(scrReference.get(i)));
            }
        }
    }
}
