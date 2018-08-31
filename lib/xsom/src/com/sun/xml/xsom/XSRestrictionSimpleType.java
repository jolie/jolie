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

import java.util.Iterator;
import java.util.Collection;
import java.util.List;

/**
 * Restriction simple type.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSRestrictionSimpleType extends XSSimpleType {
    // TODO
    
    /** Iterates facets that are specified in this step of derivation. */
    public Iterator<XSFacet> iterateDeclaredFacets();

    public Collection<? extends XSFacet> getDeclaredFacets();

    /**
     * Gets the declared facet object of the given name.
     * 
     * <p>
     * This method returns a facet object that is added in this
     * type and does not recursively check the ancestors.
     * 
     * <p>
     * For those facets that can have multiple values
     * (pattern facets and enumeration facets), this method
     * will return only the first one.
     *
     * @return
     *      Null if the facet is not specified in the last step
     *      of derivation.
     */
    XSFacet getDeclaredFacet( String name );

    /**
     * Gets the declared facets of the given name.
     *
     * This method is for those facets (such as 'pattern') that
     * can be specified multiple times on a simple type.
     *
     * @return
     *      can be empty but never be null.
     */
    List<XSFacet> getDeclaredFacets( String name );
}
