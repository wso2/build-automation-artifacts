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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * This class has been use for the visit component class and add the SCR component annotation
 */
public class ClassVisitor extends VoidVisitorAdapter<Void> {

    private SCRComponent scrComponent;
    private String classComment;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        super.visit(n, arg);
        n.getName();
        n.remove(n.getComment().get());
        if (classComment != null && !classComment.isEmpty()) {
            n.setJavadocComment(classComment);
        }
        n.addAnnotation(addComponentAnnotation(scrComponent));
    }

    public void setScrComponent(SCRComponent scrComponent) {
        this.scrComponent = scrComponent;
    }

    private NormalAnnotationExpr addComponentAnnotation(SCRComponent scrComponent) {
        NodeList<MemberValuePair> nodeList = new NodeList<>();

        NormalAnnotationExpr normalAnnotationExpr = new NormalAnnotationExpr(new Name("Component"), nodeList);
        char c = '"';
        normalAnnotationExpr.addPair("\n         name", c + scrComponent.getComponentName() + c);
        normalAnnotationExpr.addPair("\n         immediate", scrComponent.getImmediateName());
        return normalAnnotationExpr;
    }

    public void setClassComment(String classComment) {
        this.classComment = classComment;
    }
}
