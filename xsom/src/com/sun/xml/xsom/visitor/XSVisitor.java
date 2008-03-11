/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.xsom.visitor;

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSXPath;

/**
 * Visitor for {@link com.sun.xml.xsom.XSComponent}
 */
public interface XSVisitor extends XSTermVisitor, XSContentTypeVisitor {
    void annotation( XSAnnotation ann );
    void attGroupDecl( XSAttGroupDecl decl );
    void attributeDecl( XSAttributeDecl decl );
    void attributeUse( XSAttributeUse use );
    void complexType( XSComplexType type );
    void schema( XSSchema schema );
//    void schemaSet( XSSchemaSet schema );
    void facet( XSFacet facet );
    void notation( XSNotation notation );
    void identityConstraint( XSIdentityConstraint decl);
    void xpath(XSXPath xp);
}
