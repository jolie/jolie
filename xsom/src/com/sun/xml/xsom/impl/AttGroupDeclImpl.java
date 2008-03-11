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

import java.util.Iterator;

import org.xml.sax.Locator;

import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.impl.parser.DelayedRef;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;

public class AttGroupDeclImpl extends AttributesHolder implements XSAttGroupDecl
{
    public AttGroupDeclImpl( SchemaDocumentImpl _parent, AnnotationImpl _annon,
        Locator _loc, ForeignAttributesImpl _fa, String _name, WildcardImpl _wildcard ) {
        
        this(_parent,_annon,_loc,_fa,_name);
        setWildcard(_wildcard);
    }
        
    public AttGroupDeclImpl( SchemaDocumentImpl _parent, AnnotationImpl _annon,
        Locator _loc, ForeignAttributesImpl _fa, String _name ) {
            
        super(_parent,_annon,_loc,_fa,_name,false);
    }
    
    
    private WildcardImpl wildcard;
    public void setWildcard( WildcardImpl wc ) { wildcard=wc; }
    public XSWildcard getAttributeWildcard() { return wildcard; }

    public XSAttributeUse getAttributeUse( String nsURI, String localName ) {
        UName name = new UName(nsURI,localName);
        XSAttributeUse o=null;
        
        Iterator<XSAttGroupDecl> itr = iterateAttGroups();
        while(itr.hasNext() && o==null)
            o = itr.next().getAttributeUse(nsURI,localName);
        
        if(o==null)     o = attributes.get(name);
        
        return o;
    }
    
    public void redefine( AttGroupDeclImpl ag ) {
        for (Iterator<Ref.AttGroup> itr = attGroups.iterator(); itr.hasNext();) {
            DelayedRef.AttGroup r = (DelayedRef.AttGroup)itr.next();
            r.redefine(ag);
        }
    }
    
    public void visit( XSVisitor visitor ) {
        visitor.attGroupDecl(this);
    }
    public <T> T apply( XSFunction<T> function ) {
        return function.attGroupDecl(this);
    }
}
