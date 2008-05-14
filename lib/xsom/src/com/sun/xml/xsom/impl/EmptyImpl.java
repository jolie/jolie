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
package com.sun.xml.xsom.impl;

import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.visitor.XSContentTypeFunction;
import com.sun.xml.xsom.visitor.XSContentTypeVisitor;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;

/**
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class EmptyImpl extends ComponentImpl implements ContentTypeImpl {
    public EmptyImpl() { super(null,null,null,null); }
    
    public XSSimpleType asSimpleType()  { return null; }
    public XSParticle asParticle()      { return null; }
    public XSContentType asEmpty()      { return this; }
    
    public Object apply( XSContentTypeFunction function ) {
        return function.empty(this);
    }
    public Object apply( XSFunction function ) {
        return function.empty(this);
    }
    public void visit( XSVisitor visitor ) {
        visitor.empty(this);
    }
    public void visit( XSContentTypeVisitor visitor ) {
        visitor.empty(this);
    }
    
    public XSContentType getContentType() { return this; }
}