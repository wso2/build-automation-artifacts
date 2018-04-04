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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

/**
 * This class use for the visit class methods.
 */
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
