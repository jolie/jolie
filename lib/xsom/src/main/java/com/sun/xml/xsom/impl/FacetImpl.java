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

import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XmlString;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

public class FacetImpl extends ComponentImpl implements XSFacet {
    public FacetImpl( SchemaDocumentImpl owner, AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl _fa,
        String _name, XmlString _value, boolean _fixed ) {
        
        super(owner,_annon,_loc,_fa);
        
        this.name = _name;
        this.value = _value;
        this.fixed = _fixed;
    }
    
    private final String name;
    public String getName() { return name; }
    
    private final XmlString value;
    public XmlString getValue() { return value; }

    private boolean fixed;
    public boolean isFixed() { return fixed; }
    
    
    public void visit( XSVisitor visitor ) {
        visitor.facet(this);
    }
    public Object apply( XSFunction function ) {
        return function.facet(this);
    }
}
