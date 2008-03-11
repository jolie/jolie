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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.NamespaceContext;

import org.xml.sax.Locator;

import com.sun.xml.xsom.SCD;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.parser.SchemaDocument;
import com.sun.xml.xsom.util.ComponentNameFunction;

public abstract class ComponentImpl implements XSComponent
{
    protected ComponentImpl( SchemaDocumentImpl _owner, AnnotationImpl _annon, Locator _loc, ForeignAttributesImpl fa ) {
        this.ownerDocument = _owner;
        this.annotation = _annon;
        this.locator = _loc;
        this.foreignAttributes = fa;
    }

    protected final SchemaDocumentImpl ownerDocument;
    public SchemaImpl getOwnerSchema() {
        if(ownerDocument==null)
            return null;
        else
            return ownerDocument.getSchema();
    }

    public XSSchemaSet getRoot() {
        if(ownerDocument==null)
            return null;
        else
            return getOwnerSchema().getRoot();
    }

    public SchemaDocument getSourceDocument() {
        return ownerDocument;
    }

    private AnnotationImpl annotation;
    public final XSAnnotation getAnnotation() { return annotation; }

    public XSAnnotation getAnnotation(boolean createIfNotExist) {
        if(createIfNotExist && annotation==null) {
            annotation = new AnnotationImpl();
        }
        return annotation;
    }

    private final Locator locator;
    public final Locator getLocator() { return locator; }

    /**
     * Either {@link ForeignAttributesImpl} or {@link List}.
     *
     * Initially it's {@link ForeignAttributesImpl}, but it's lazily turned into
     * a list when necessary.
     */
    private Object foreignAttributes;

    public List<ForeignAttributesImpl> getForeignAttributes() {
        Object t = foreignAttributes;

        if(t==null)
            return Collections.EMPTY_LIST;

        if(t instanceof List)
            return (List)t;

        t = foreignAttributes = convertToList((ForeignAttributesImpl)t);
        return (List)t;
    }

    public String getForeignAttribute(String nsUri, String localName) {
        for( ForeignAttributesImpl fa : getForeignAttributes() ) {
            String v = fa.getValue(nsUri,localName);
            if(v!=null) return v;
        }
        return null;
    }

    private List<ForeignAttributesImpl> convertToList(ForeignAttributesImpl fa) {
        List<ForeignAttributesImpl> lst = new ArrayList<ForeignAttributesImpl>();
        while(fa!=null) {
            lst.add(fa);
            fa = fa.next;
        }
        return Collections.unmodifiableList(lst);
    }

    public Collection<XSComponent> select(String scd, NamespaceContext nsContext) {
        try {
            return SCD.create(scd,nsContext).select(this);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public XSComponent selectSingle(String scd, NamespaceContext nsContext) {
        try {
            return SCD.create(scd,nsContext).selectSingle(this);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public String toString() {
        return apply(new ComponentNameFunction());
    }
}
