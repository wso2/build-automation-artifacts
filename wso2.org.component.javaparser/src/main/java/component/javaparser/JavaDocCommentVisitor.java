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

import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * This class use for the read javadoc comment in class
 */
public class JavaDocCommentVisitor extends VoidVisitorAdapter<Void> {

    private String docComment;

    @Override
    public void visit(JavadocComment n, Void arg) {
        super.visit(n, arg);
        this.docComment = n.getContent().toString();
    }

    public String getDocContent() {
        return docComment;
    }

}
