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
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.impl.parser.DelayedRef;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.impl.scd.Iterators;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;

public class ComplexTypeImpl extends AttributesHolder implements XSComplexType, Ref.ComplexType
{
    public ComplexTypeImpl( SchemaDocumentImpl _parent,
        AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl _fa,
        String _name, boolean _anonymous,
        
        boolean _abstract, int _derivationMethod,
        Ref.Type _base, int _final, int _block, boolean _mixed ) {
        
        super(_parent,_annon,_loc,_fa,_name,_anonymous);
        
        if(_base==null)
            throw new IllegalArgumentException();
            
        this._abstract = _abstract;
        this.derivationMethod = _derivationMethod;
        this.baseType = _base;
        this.finalValue = _final;
        this.blockValue = _block;
        this.mixed = _mixed;
    }
    
    public XSComplexType asComplexType(){ return this; }

    public boolean isDerivedFrom(XSType t) {
        XSType x = this;
        while(true) {
            if(t==x)
                return true;
            XSType s = x.getBaseType();
            if(s==x)
                return false;
            x = s;
        }
    }

    public XSSimpleType  asSimpleType()    { return null; }
    public final boolean isSimpleType() { return false; }
    public final boolean isComplexType(){ return true; }
    
    private int derivationMethod;
    public int getDerivationMethod() { return derivationMethod; }
    
    private Ref.Type baseType;
    public XSType getBaseType() { return baseType.getType(); }

    /**
     * Called when this complex type redefines the specified complex type.
     */
    public void redefine( ComplexTypeImpl ct ) {
        if( baseType instanceof DelayedRef )
            ((DelayedRef)baseType).redefine(ct);
        else
            this.baseType = ct;
        ct.redefinedBy = this;
        redefiningCount = (short)(ct.redefiningCount+1);
    }

    /**
     * Number of times this component redefines other components.
     */
    private short redefiningCount = 0;

    private ComplexTypeImpl redefinedBy = null;

    public XSComplexType getRedefinedBy() {
        return redefinedBy;
    }

    public int getRedefinedCount() {
        int i=0;
        for( ComplexTypeImpl ct=this.redefinedBy; ct!=null; ct=ct.redefinedBy)
            i++;
        return i;
    }


    private XSElementDecl scope;
    public XSElementDecl getScope() { return scope; }
    public void setScope( XSElementDecl _scope ) { this.scope=_scope; }

    private final boolean _abstract;
    public boolean isAbstract() { return _abstract; }
    
    private WildcardImpl localAttWildcard;
    /**
     * Set the local attribute wildcard.
     */
    public void setWildcard( WildcardImpl wc ) {
        this.localAttWildcard = wc;
    }
    public XSWildcard getAttributeWildcard() {
        WildcardImpl complete = localAttWildcard;
        
        Iterator<XSAttGroupDecl> itr = iterateAttGroups();
        while( itr.hasNext() ) {
            WildcardImpl w = (WildcardImpl)(itr.next().getAttributeWildcard());
            
            if(w==null)     continue;
            
            if(complete==null)
                complete = w;
            else
                // TODO: the spec says it's intersection,
                // but I think it has to be union.
                complete = complete.union(ownerDocument,w);
        }
        
        if( getDerivationMethod()==RESTRICTION )    return complete;
        
        WildcardImpl base=null;
        XSType baseType = getBaseType();
        if(baseType.asComplexType()!=null)
            base = (WildcardImpl)baseType.asComplexType().getAttributeWildcard();
        
        if(complete==null)  return base;
        if(base==null)      return complete;
        
        return complete.union(ownerDocument,base);
    }

    private final int finalValue;
    public boolean isFinal( int derivationMethod ) {
        return (finalValue&derivationMethod)!=0;
    }
    
    private final int blockValue;
    public boolean isSubstitutionProhibited( int method ) {
        return (blockValue&method)!=0;
    }
    
    
    private Ref.ContentType contentType;
    public void setContentType( Ref.ContentType v ) { contentType = v; }
    public XSContentType getContentType() { return contentType.getContentType(); }
    
    private XSContentType explicitContent;
    public void setExplicitContent( XSContentType v ) {
        this.explicitContent = v;
    }
    public XSContentType getExplicitContent() { return explicitContent; }
    
    private final boolean mixed;
    public boolean isMixed() { return mixed; }




    public XSAttributeUse getAttributeUse( String nsURI, String localName ) {
        UName name = new UName(nsURI,localName);
        
        if(prohibitedAtts.contains(name))       return null;
        
        XSAttributeUse o = attributes.get(name);
        
        
        if(o==null) {
            Iterator<XSAttGroupDecl> itr = iterateAttGroups();
            while(itr.hasNext() && o==null)
                o = itr.next().getAttributeUse(nsURI,localName);
        }
        
        if(o==null) {
            XSType base = getBaseType();
            if(base.asComplexType()!=null)
                o = base.asComplexType().getAttributeUse(nsURI,localName);
        }
        
        return o;
    }

    public Iterator<XSAttributeUse> iterateAttributeUses() {
        
        XSComplexType baseType = getBaseType().asComplexType();
        
        if( baseType==null )    return super.iterateAttributeUses();
        
        return new Iterators.Union<XSAttributeUse>(
            new Iterators.Filter<XSAttributeUse>(baseType.iterateAttributeUses()) {
                protected boolean matches(XSAttributeUse value) {
                    XSAttributeDecl u = value.getDecl();
                    UName n = new UName(u.getTargetNamespace(),u.getName());
                    return !prohibitedAtts.contains(n);
                }
            },
            super.iterateAttributeUses() );
    }
    
    
    public XSType[] listSubstitutables() {
        return Util.listSubstitutables(this);
    }
    


    public void visit( XSVisitor visitor ) {
        visitor.complexType(this);
    }
    public <T> T apply( XSFunction<T> function ) {
        return function.complexType(this);
    }

    // Ref.ComplexType implementation
    public XSComplexType getType() { return this; }
}
