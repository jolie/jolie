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

import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.impl.scd.Iterators;
import com.sun.xml.xsom.impl.Ref.AttGroup;
import org.xml.sax.Locator;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public abstract class AttributesHolder extends DeclarationImpl {
    
    protected AttributesHolder( SchemaDocumentImpl _parent, AnnotationImpl _annon,
        Locator loc, ForeignAttributesImpl _fa, String _name, boolean _anonymous ) {
        
        super(_parent,_annon,loc,_fa,_parent.getTargetNamespace(),_name,_anonymous);
    }
    
    /** set the local wildcard. */
    public abstract void setWildcard(WildcardImpl wc);
    
    /**
     * Local attribute use.
     * It has to be {@link TreeMap} or otherwise we cannot guarantee
     * the order of iteration.
     */
    protected final Map<UName,AttributeUseImpl> attributes = new TreeMap<UName,AttributeUseImpl>(UName.comparator);
    public void addAttributeUse( UName name, AttributeUseImpl a ) {
        attributes.put( name, a );
    }
    /** prohibited attributes. */
    protected final Set<UName> prohibitedAtts = new HashSet<UName>();
    public void addProhibitedAttribute( UName name ) {
        prohibitedAtts.add(name);
    }
    public List<XSAttributeUse> getAttributeUses() {
        // TODO: this is fairly inefficient
        List<XSAttributeUse> v = new ArrayList<XSAttributeUse>();
        v.addAll(attributes.values());
        for( XSAttGroupDecl agd : getAttGroups() )
            v.addAll(agd.getAttributeUses());
        return v;
    }
    public Iterator<XSAttributeUse> iterateAttributeUses() {
        return getAttributeUses().iterator();
    }



    public XSAttributeUse getDeclaredAttributeUse( String nsURI, String localName ) {
        return attributes.get(new UName(nsURI,localName));
    }
    
    public Iterator<AttributeUseImpl> iterateDeclaredAttributeUses() {
        return attributes.values().iterator();
    }

    public Collection<AttributeUseImpl> getDeclaredAttributeUses() {
        return attributes.values();
    }


    /** {@link Ref.AttGroup}s that are directly refered from this. */
    protected final Set<Ref.AttGroup> attGroups = new HashSet<Ref.AttGroup>();
    
    public void addAttGroup( Ref.AttGroup a ) { attGroups.add(a); }
    
    // Iterates all AttGroups which are directly referenced from this component
    // this does not iterate att groups referenced from the base type
    public Iterator<XSAttGroupDecl> iterateAttGroups() {
        return new Iterators.Adapter<XSAttGroupDecl,Ref.AttGroup>(attGroups.iterator()) {
            protected XSAttGroupDecl filter(AttGroup u) {
                return u.get();
            }
        };
    }

    public Set<XSAttGroupDecl> getAttGroups() {
        return new AbstractSet<XSAttGroupDecl>() {
            public Iterator<XSAttGroupDecl> iterator() {
                return iterateAttGroups();
            }

            public int size() {
                return attGroups.size();
            }
        };
    }
}
