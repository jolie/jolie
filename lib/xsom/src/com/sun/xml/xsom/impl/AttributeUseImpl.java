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

import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

public class AttributeUseImpl extends ComponentImpl implements XSAttributeUse
{
    public AttributeUseImpl( SchemaDocumentImpl owner, AnnotationImpl ann, Locator loc, ForeignAttributesImpl fa, Ref.Attribute _decl,
        XmlString def, XmlString fixed, boolean req ) {
        
        super(owner,ann,loc,fa);
        
        this.att = _decl;
        this.defaultValue = def;
        this.fixedValue = fixed;
        this.required = req;
    }
    
    private final Ref.Attribute att;    
    public XSAttributeDecl getDecl() { return att.getAttribute(); }
    
    private final XmlString defaultValue;
    public XmlString getDefaultValue() {
        if( defaultValue!=null )    return defaultValue;
        else                        return getDecl().getDefaultValue();
    }
    
    private final XmlString fixedValue;
    public XmlString getFixedValue() {
        if( fixedValue!=null )      return fixedValue;
        else                        return getDecl().getFixedValue();
    }
    
    private final boolean required;
    public boolean isRequired() { return required; }
    
    public <T> T apply( XSFunction<T> f ) {
        return f.attributeUse(this);
    }
    public void visit( XSVisitor v ) {
        v.attributeUse(this);
    }
}
