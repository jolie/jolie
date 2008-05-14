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
package com.sun.xml.xsom.util;

import java.util.Locale;
import java.util.ResourceBundle;

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSModelGroupDecl;
import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSXPath;
import com.sun.xml.xsom.visitor.XSFunction;

/**
 * Gets the human-readable name of a schema component.
 * 
 * <p>
 * This is a function object that returns {@link String}.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class NameGetter implements XSFunction<String> {
    /**
     * Initializes a NameGetter so that it will return
     * messages in the specified locale.
     */
    public NameGetter( Locale _locale ) {
        this.locale = _locale;
    }
    
    private final Locale locale;
    
    /**
     * An instance that gets names in the default locale.
     * This instance is provided just for convenience.
     */
    public final static XSFunction<?> theInstance = new NameGetter(null);
    
    /**
     * Gets the name of the specified component in the default locale.
     * This method is just a wrapper.
     */
    public static String get( XSComponent comp ) {
        return (String)comp.apply(theInstance);
    }
    
    
    public String annotation(XSAnnotation ann) {
        return localize("annotation");
    }

    public String attGroupDecl(XSAttGroupDecl decl) {
        return localize("attGroupDecl");
    }

    public String attributeUse(XSAttributeUse use) {
        return localize("attributeUse");
    }

    public String attributeDecl(XSAttributeDecl decl) {
        return localize("attributeDecl");
    }

    public String complexType(XSComplexType type) {
        return localize("complexType");
    }

    public String schema(XSSchema schema) {
        return localize("schema");
    }

    public String facet(XSFacet facet) {
        return localize("facet");
    }

    public String simpleType(XSSimpleType simpleType) {
        return localize("simpleType");
    }

    public String particle(XSParticle particle) {
        return localize("particle");
    }

    public String empty(XSContentType empty) {
        return localize("empty");
    }

    public String wildcard(XSWildcard wc) {
        return localize("wildcard");
    }

    public String modelGroupDecl(XSModelGroupDecl decl) {
        return localize("modelGroupDecl");
    }

    public String modelGroup(XSModelGroup group) {
         return localize("modelGroup");
    }

    public String elementDecl(XSElementDecl decl) {
        return localize("elementDecl");
    }
    
    public String notation( XSNotation n ) {
        return localize("notation");
    }

    public String identityConstraint(XSIdentityConstraint decl) {
        return localize("idConstraint");
    }

    public String xpath(XSXPath xpath) {
        return localize("xpath");
    }

    private String localize( String key ) {
        ResourceBundle rb;
        
        if(locale==null)
            rb = ResourceBundle.getBundle(NameGetter.class.getName());
        else
            rb = ResourceBundle.getBundle(NameGetter.class.getName(),locale);
        
        return rb.getString(key);
    }
}
