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
package com.sun.xml.xsom;

import com.sun.xml.xsom.parser.SchemaDocument;

import java.util.Iterator;
import java.util.Map;

/**
 * Schema.
 * 
 * Container of declarations that belong to the same target namespace.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSSchema extends XSComponent
{
    /**
     * Gets the target namespace of the schema.
     *
     * @return
     *      can be empty, but never be null.
     */
    String getTargetNamespace();

    /**
     * Gets all the {@link XSAttributeDecl}s in this schema
     * keyed by their local names.
     */
    Map<String,XSAttributeDecl> getAttributeDecls();
    Iterator<XSAttributeDecl> iterateAttributeDecls();
    XSAttributeDecl getAttributeDecl(String localName);

    /**
     * Gets all the {@link XSElementDecl}s in this schema.
     */
    Map<String,XSElementDecl> getElementDecls();
    Iterator<XSElementDecl> iterateElementDecls();
    XSElementDecl getElementDecl(String localName);

    /**
     * Gets all the {@link XSAttGroupDecl}s in this schema.
     */
    Map<String,XSAttGroupDecl> getAttGroupDecls();
    Iterator<XSAttGroupDecl> iterateAttGroupDecls();
    XSAttGroupDecl getAttGroupDecl(String localName);

    /**
     * Gets all the {@link XSModelGroupDecl}s in this schema.
     */
    Map<String,XSModelGroupDecl> getModelGroupDecls();
    Iterator<XSModelGroupDecl> iterateModelGroupDecls();
    XSModelGroupDecl getModelGroupDecl(String localName);

    /**
     * Gets all the {@link XSType}s in this schema (union of
     * {@link #getSimpleTypes()} and {@link #getComplexTypes()}
     */
    Map<String,XSType> getTypes();
    Iterator<XSType> iterateTypes();
    XSType getType(String localName);

    /**
     * Gets all the {@link XSSimpleType}s in this schema.
     */
    Map<String,XSSimpleType> getSimpleTypes();
    Iterator<XSSimpleType> iterateSimpleTypes();
    XSSimpleType getSimpleType(String localName);

    /**
     * Gets all the {@link XSComplexType}s in this schema.
     */
    Map<String,XSComplexType> getComplexTypes();
    Iterator<XSComplexType> iterateComplexTypes();
    XSComplexType getComplexType(String localName);

    /**
     * Gets all the {@link XSNotation}s in this schema.
     */
    Map<String,XSNotation> getNotations();
    Iterator<XSNotation> iterateNotations();
    XSNotation getNotation(String localName);

    /**
     * Gets all the {@link XSIdentityConstraint}s in this schema,
     * keyed by their names.
     */
    Map<String,XSIdentityConstraint> getIdentityConstraints();

    /**
     * Gets the identity constraint of the given name, or null if not found.
     */
    XSIdentityConstraint getIdentityConstraint(String localName);
    
    /**
     * The @elementFormDefault value of the schema.
     * True if local elements are qualified by default.
     */
    public boolean elementFormDefault();
    
    /**
     * The @attributeFormDefault value of the schema.
     * True if local attributes are qualified by default.
     */
    public boolean attributeFormDefault();

    /**
     * Sine an {@link XSSchema} is not necessarily defined in
     * one schema document (for example one schema can span across
     * many documents through &lt;xs:include>s.),
     * so this method always returns null.
     *
     * @deprecated
     *      Since this method always returns null, if you are calling
     *      this method from {@link XSSchema} and not from {@link XSComponent},
     *      there's something wrong with your code.
     */
    SchemaDocument getSourceDocument();

    /**
     * Gets the root schema set that includes this schema.
     *
     * @return never null.
     */
    XSSchemaSet getRoot();
}
