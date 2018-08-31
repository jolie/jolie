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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

import org.xml.sax.Locator;

import com.sun.xml.xsom.ForeignAttributes;
import com.sun.xml.xsom.SCD;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.SchemaDocument;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;

public class SchemaImpl implements XSSchema
{
	private boolean elementFormDefault, attributeFormDefault;
	
    public SchemaImpl(SchemaSetImpl _parent, Locator loc, String tns, boolean elementFormDefault, boolean attributeFormDefault) {
        if (tns == null)
            // the empty string must be used.
            throw new IllegalArgumentException();
        this.targetNamespace = tns;
        this.parent = _parent;
        this.locator = loc;
        this.elementFormDefault = elementFormDefault;
        this.attributeFormDefault = attributeFormDefault;
    }
    
    public boolean elementFormDefault()
    {
    	return elementFormDefault;
    }
    
    public boolean attributeFormDefault()
    {
    	return attributeFormDefault;
    }

    public SchemaDocument getSourceDocument() {
        return null;
    }

    public SchemaSetImpl getRoot() {
        return parent;
    }

    protected final SchemaSetImpl parent;

    private final String targetNamespace;
    public String getTargetNamespace() {
        return targetNamespace;
    }

    public XSSchema getOwnerSchema() {
        return this;
    }

    private XSAnnotation annotation;
    public void setAnnotation(XSAnnotation a) {
        annotation = a;
    }
    public XSAnnotation getAnnotation() {
        return annotation;
    }

    public XSAnnotation getAnnotation(boolean createIfNotExist) {
        if(createIfNotExist && annotation==null)
            annotation = new AnnotationImpl();
        return annotation;
    }

    // it's difficult to determine the source location for the schema
    // component as one schema can be defined across multiple files.
    // so this locator might not correctly reflect all the locations
    // where the schema is defined.
    // but partial information would be better than nothing.

    private final Locator locator;
    public Locator getLocator() {
        return locator;
    }


    private final Map<String,XSAttributeDecl> atts = new HashMap<String,XSAttributeDecl>();
    private final Map<String,XSAttributeDecl> attsView = Collections.unmodifiableMap(atts);
    public void addAttributeDecl(XSAttributeDecl newDecl) {
        atts.put(newDecl.getName(), newDecl);
    }
    public Map<String,XSAttributeDecl> getAttributeDecls() {
        return attsView;
    }
    public XSAttributeDecl getAttributeDecl(String name) {
        return atts.get(name);
    }
    public Iterator<XSAttributeDecl> iterateAttributeDecls() {
        return atts.values().iterator();
    }

    private final Map<String,XSElementDecl> elems = new HashMap<String,XSElementDecl>();
    private final Map<String,XSElementDecl> elemsView = Collections.unmodifiableMap(elems);
    public void addElementDecl(XSElementDecl newDecl) {
        elems.put(newDecl.getName(), newDecl);
    }
    public Map<String,XSElementDecl> getElementDecls() {
        return elemsView;
    }
    public XSElementDecl getElementDecl(String name) {
        return elems.get(name);
    }
    public Iterator<XSElementDecl> iterateElementDecls() {
        return elems.values().iterator();
    }

    private final Map<String,XSAttGroupDecl> attGroups = new HashMap<String,XSAttGroupDecl>();
    private final Map<String,XSAttGroupDecl> attGroupsView = Collections.unmodifiableMap(attGroups);
    public void addAttGroupDecl(XSAttGroupDecl newDecl, boolean overwrite) {
        if(overwrite || !attGroups.containsKey(newDecl.getName()))
            attGroups.put(newDecl.getName(), newDecl);
    }
    public Map<String,XSAttGroupDecl> getAttGroupDecls() {
        return attGroupsView;
    }
    public XSAttGroupDecl getAttGroupDecl(String name) {
        return attGroups.get(name);
    }
    public Iterator<XSAttGroupDecl> iterateAttGroupDecls() {
        return attGroups.values().iterator();
    }


    private final Map<String,XSNotation> notations = new HashMap<String,XSNotation>();
    private final Map<String,XSNotation> notationsView = Collections.unmodifiableMap(notations);
    public void addNotation( XSNotation newDecl ) {
        notations.put( newDecl.getName(), newDecl );
    }
    public Map<String,XSNotation> getNotations() {
        return notationsView;
    }
    public XSNotation getNotation( String name ) {
        return notations.get(name);
    }
    public Iterator<XSNotation> iterateNotations() {
        return notations.values().iterator();
    }

    private final Map<String,XSModelGroupDecl> modelGroups = new HashMap<String,XSModelGroupDecl>();
    private final Map<String,XSModelGroupDecl> modelGroupsView = Collections.unmodifiableMap(modelGroups);
    public void addModelGroupDecl(XSModelGroupDecl newDecl, boolean overwrite) {
        if(overwrite || !modelGroups.containsKey(newDecl.getName()))
            modelGroups.put(newDecl.getName(), newDecl);
    }
    public Map<String,XSModelGroupDecl> getModelGroupDecls() {
        return modelGroupsView;
    }
    public XSModelGroupDecl getModelGroupDecl(String name) {
        return modelGroups.get(name);
    }
    public Iterator<XSModelGroupDecl> iterateModelGroupDecls() {
        return modelGroups.values().iterator();
    }


    private final Map<String,XSIdentityConstraint> idConstraints = new HashMap<String,XSIdentityConstraint>();
    private final Map<String,XSIdentityConstraint> idConstraintsView = Collections.unmodifiableMap(idConstraints);

    protected void addIdentityConstraint(IdentityConstraintImpl c) {
        idConstraints.put(c.getName(),c);
    }

    public Map<String, XSIdentityConstraint> getIdentityConstraints() {
        return idConstraintsView;
    }

    public XSIdentityConstraint getIdentityConstraint(String localName) {
        return idConstraints.get(localName);
    }

    private final Map<String,XSType> allTypes = new HashMap<String,XSType>();
    private final Map<String,XSType> allTypesView = Collections.unmodifiableMap(allTypes);

    private final Map<String,XSSimpleType> simpleTypes = new HashMap<String,XSSimpleType>();
    private final Map<String,XSSimpleType> simpleTypesView = Collections.unmodifiableMap(simpleTypes);
    public void addSimpleType(XSSimpleType newDecl, boolean overwrite) {
        if(overwrite || !simpleTypes.containsKey(newDecl.getName())) {
            simpleTypes.put(newDecl.getName(), newDecl);
            allTypes.put(newDecl.getName(), newDecl);
        }
    }
    public Map<String,XSSimpleType> getSimpleTypes() {
        return simpleTypesView;
    }
    public XSSimpleType getSimpleType(String name) {
        return simpleTypes.get(name);
    }
    public Iterator<XSSimpleType> iterateSimpleTypes() {
        return simpleTypes.values().iterator();
    }

    private final Map<String,XSComplexType> complexTypes = new HashMap<String,XSComplexType>();
    private final Map<String,XSComplexType> complexTypesView = Collections.unmodifiableMap(complexTypes);
    public void addComplexType(XSComplexType newDecl, boolean overwrite) {
        if(overwrite || !complexTypes.containsKey(newDecl.getName())) {
            complexTypes.put(newDecl.getName(), newDecl);
            allTypes.put(newDecl.getName(), newDecl);
        }
    }
    public Map<String,XSComplexType> getComplexTypes() {
        return complexTypesView;
    }
    public XSComplexType getComplexType(String name) {
        return complexTypes.get(name);
    }
    public Iterator<XSComplexType> iterateComplexTypes() {
        return complexTypes.values().iterator();
    }

    public Map<String,XSType> getTypes() {
        return allTypesView;
    }
    public XSType getType(String name) {
        return allTypes.get(name);
    }
    public Iterator<XSType> iterateTypes() {
        return allTypes.values().iterator();
    }

    public void visit(XSVisitor visitor) {
        visitor.schema(this);
    }

    public <T> T apply(XSFunction<T> function) {
        return function.schema(this);
    }

    /**
     * Lazily created list of {@link ForeignAttributesImpl}s.
     */
    private List<ForeignAttributes> foreignAttributes = null;
    private List<ForeignAttributes> readOnlyForeignAttributes = null;

    public void addForeignAttributes(ForeignAttributesImpl fa) {
        if(foreignAttributes==null)
            foreignAttributes = new ArrayList<ForeignAttributes>();
        foreignAttributes.add(fa);
    }

    public List<ForeignAttributes> getForeignAttributes() {
        if(readOnlyForeignAttributes==null) {
            if(foreignAttributes==null)
                readOnlyForeignAttributes = Collections.emptyList();
            else
                readOnlyForeignAttributes = Collections.unmodifiableList(foreignAttributes);
        }
        return readOnlyForeignAttributes;
    }

    public String getForeignAttribute(String nsUri, String localName) {
        for( ForeignAttributes fa : getForeignAttributes() ) {
            String v = fa.getValue(nsUri,localName);
            if(v!=null) return v;
        }
        return null;
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
}