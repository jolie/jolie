package com.sun.xml.xsom.util;

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.XSComplexType;
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
 * Extract the name of the components.
 * 
 * @author <ul><li>Ryan Shoemaker, Sun Microsystems, Inc.</li></ul>
 * @version $Revision: 1.1 $
 */
public class ComponentNameFunction implements XSFunction<String> {

    // delegate to this object to get the localized name of the component type
    private NameGetter nameGetter = new NameGetter(null);
    
    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#annotation(XSAnnotation)
     */
    public String annotation(XSAnnotation ann) {
        // unnamed component
        return nameGetter.annotation( ann );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#attGroupDecl(XSAttGroupDecl)
     */
    public String attGroupDecl(XSAttGroupDecl decl) {
        String name = decl.getName();
        if( name == null ) name = "";
        return name + " " + nameGetter.attGroupDecl( decl );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#attributeDecl(XSAttributeDecl)
     */
    public String attributeDecl(XSAttributeDecl decl) {
        String name = decl.getName();
        if( name == null ) name = "";
        return name + " " + nameGetter.attributeDecl( decl );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#attributeUse(XSAttributeUse)
     */
    public String attributeUse(XSAttributeUse use) {
        // unnamed component
        return nameGetter.attributeUse( use );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#complexType(XSComplexType)
     */
    public String complexType(XSComplexType type) {
        String name = type.getName();
        if( name == null ) name = "anonymous";
        return name + " " + nameGetter.complexType( type );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#schema(XSSchema)
     */
    public String schema(XSSchema schema) {
        return nameGetter.schema( schema ) + " \"" + schema.getTargetNamespace()+"\"";
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#facet(XSFacet)
     */
    public String facet(XSFacet facet) {
        String name = facet.getName();
        if( name == null ) name = "";
        return name + " " + nameGetter.facet( facet );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSFunction#notation(XSNotation)
     */
    public String notation(XSNotation notation) {
        String name = notation.getName();
        if( name == null ) name = "";
        return name + " " + nameGetter.notation( notation );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSContentTypeFunction#simpleType(XSSimpleType)
     */
    public String simpleType(XSSimpleType simpleType) {
        String name = simpleType.getName();
        if( name == null ) name = "anonymous";
        return name + " " + nameGetter.simpleType( simpleType );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSContentTypeFunction#particle(XSParticle)
     */
    public String particle(XSParticle particle) {
        // unnamed component
        return nameGetter.particle( particle );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSContentTypeFunction#empty(XSContentType)
     */
    public String empty(XSContentType empty) {
        // unnamed component
        return nameGetter.empty( empty );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSTermFunction#wildcard(XSWildcard)
     */
    public String wildcard(XSWildcard wc) {
        // unnamed component
        return nameGetter.wildcard( wc );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSTermFunction#modelGroupDecl(XSModelGroupDecl)
     */
    public String modelGroupDecl(XSModelGroupDecl decl) {
        String name = decl.getName();
        if( name == null ) name = "";
        return name + " " + nameGetter.modelGroupDecl( decl );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSTermFunction#modelGroup(XSModelGroup)
     */
    public String modelGroup(XSModelGroup group) {
        // unnamed component
        return nameGetter.modelGroup( group );
    }

    /**
     * @see com.sun.xml.xsom.visitor.XSTermFunction#elementDecl(XSElementDecl)
     */
    public String elementDecl(XSElementDecl decl) {
        String name = decl.getName();
        if( name == null ) name = "";
        return name + " " + nameGetter.elementDecl( decl );
    }

    public String identityConstraint(XSIdentityConstraint decl) {
        return decl.getName()+" "+nameGetter.identityConstraint(decl);
    }

    public String xpath(XSXPath xpath) {
        return nameGetter.xpath(xpath);
    }
}
