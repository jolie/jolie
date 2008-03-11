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
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSVariety;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSSimpleTypeFunction;
import com.sun.xml.xsom.visitor.XSSimpleTypeVisitor;
import org.xml.sax.Locator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RestrictionSimpleTypeImpl extends SimpleTypeImpl implements XSRestrictionSimpleType {

    public RestrictionSimpleTypeImpl( SchemaDocumentImpl _parent,
                                      AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl _fa,
                                      String _name, boolean _anonymous, Set<XSVariety> finalSet,
                                      Ref.SimpleType _baseType ) {

        super( _parent, _annon, _loc, _fa, _name, _anonymous, finalSet, _baseType );
    }


    private final List<XSFacet> facets = new ArrayList<XSFacet>();
    public void addFacet( XSFacet facet ) {
        facets.add(facet);
    }
    public Iterator<XSFacet> iterateDeclaredFacets() {
        return facets.iterator();
    }

    public Collection<? extends XSFacet> getDeclaredFacets() {
        return facets;
    }

    public XSFacet getDeclaredFacet( String name ) {
        int len = facets.size();
        for( int i=0; i<len; i++ ) {
            XSFacet f = facets.get(i);
            if(f.getName().equals(name))
                return f;
        }
        return null;
    }

    public List<XSFacet> getDeclaredFacets(String name) {
        List<XSFacet> r = new ArrayList<XSFacet>();
        for( XSFacet f : facets )
            if(f.getName().equals(name))
                r.add(f);
        return r;
    }

    public XSFacet getFacet( String name ) {
        XSFacet f = getDeclaredFacet(name);
        if(f!=null)     return f;

        // none was found on this datatype. check the base type.
        return getSimpleBaseType().getFacet(name);
    }

    public XSVariety getVariety() { return getSimpleBaseType().getVariety(); }

    public XSSimpleType getPrimitiveType() {
        if(isPrimitive())       return this;
        return getSimpleBaseType().getPrimitiveType();
    }

    public boolean isPrimitive() {
        return getSimpleBaseType()==getOwnerSchema().getRoot().anySimpleType;
    }

    public void visit( XSSimpleTypeVisitor visitor ) {
        visitor.restrictionSimpleType(this);
    }
    public Object apply( XSSimpleTypeFunction function ) {
        return function.restrictionSimpleType(this);
    }

    public boolean isRestriction() { return true; }
    public XSRestrictionSimpleType asRestriction() { return this; }
}
