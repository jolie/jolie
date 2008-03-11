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
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

public class AttributeDeclImpl extends DeclarationImpl implements XSAttributeDecl, Ref.Attribute
{
    public AttributeDeclImpl( SchemaDocumentImpl owner,
        String _targetNamespace, String _name,
        AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl _fa, boolean _anonymous,
        XmlString _defValue, XmlString _fixedValue,
        Ref.SimpleType _type ) {
        
        super(owner,_annon,_loc,_fa,_targetNamespace,_name,_anonymous);
        
        if(_name==null) // assertion failed.
            throw new IllegalArgumentException();
        
        this.defaultValue = _defValue;
        this.fixedValue = _fixedValue;
        this.type = _type;
    }
    
    private final Ref.SimpleType type;
    public XSSimpleType getType() { return type.getType(); }

    private final XmlString defaultValue;
    public XmlString getDefaultValue() { return defaultValue; }
    
    private final XmlString fixedValue;
    public XmlString getFixedValue() { return fixedValue; }
    
    public void visit( XSVisitor visitor ) {
        visitor.attributeDecl(this);
    }
    public <T> T apply( XSFunction<T> function ) {
        return function.attributeDecl(this);
    }


    // Ref.Attribute implementation
    public XSAttributeDecl getAttribute() { return this; }
 }
