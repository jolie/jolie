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
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.impl.parser.DelayedRef;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.parser.SchemaDocument;
import com.sun.xml.xsom.visitor.XSContentTypeFunction;
import com.sun.xml.xsom.visitor.XSContentTypeVisitor;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

import java.util.List;

public class ParticleImpl extends ComponentImpl implements XSParticle, ContentTypeImpl
{
    public ParticleImpl( SchemaDocumentImpl owner, AnnotationImpl _ann,
        Ref.Term _term, Locator _loc, int _maxOccurs, int _minOccurs ) {
            
        super(owner,_ann,_loc,null);
        this.term = _term;
        this.maxOccurs = _maxOccurs;
        this.minOccurs = _minOccurs;
    }
    public ParticleImpl( SchemaDocumentImpl owner, AnnotationImpl _ann, Ref.Term _term, Locator _loc ) {
        this(owner,_ann,_term,_loc,1,1);
    }
    
    private Ref.Term term;
    public XSTerm getTerm() { return term.getTerm(); }
    
    private int maxOccurs;
    public int getMaxOccurs() { return maxOccurs; }

    public boolean isRepeated() {
        return maxOccurs!=0 && maxOccurs!=1;
    }

    private int minOccurs;
    public int getMinOccurs() { return minOccurs; }
    
    
    public void redefine(ModelGroupDeclImpl oldMG) {
        if( term instanceof ModelGroupImpl ) {
            ((ModelGroupImpl)term).redefine(oldMG);
            return;
        }
        if( term instanceof DelayedRef.ModelGroup ) {
            ((DelayedRef)term).redefine(oldMG);
        }
    }
    
    
    public XSSimpleType asSimpleType()  { return null; }
    public XSParticle asParticle()      { return this; }
    public XSContentType asEmpty()      { return null; }
    
    
    public final Object apply( XSFunction function ) {
        return function.particle(this);
    }
    public final Object apply( XSContentTypeFunction function ) {
        return function.particle(this);
    }
    public final void visit( XSVisitor visitor ) {
        visitor.particle(this);
    }
    public final void visit( XSContentTypeVisitor visitor ) {
        visitor.particle(this);
    }

    // Ref.ContentType implementation
    public XSContentType getContentType() { return this; }

    /**
     * Foreign attribuets are considered to be on terms.
     *
     * REVISIT: is this a good design?
     */
    public List getForeignAttributes() {
        return getTerm().getForeignAttributes();
    }
}
