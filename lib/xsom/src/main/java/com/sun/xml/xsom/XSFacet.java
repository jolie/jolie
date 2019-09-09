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

/**
 * Facet for a simple type.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSFacet extends XSComponent
{
    /** Gets the name of the facet, such as "length". */
    String getName();
    
    /** Gets the value of the facet. */
    XmlString getValue();
    
    /** Returns true if this facet is "fixed". */
    boolean isFixed();
    
    
    // well-known facet name constants
    final static String FACET_LENGTH            = "length";
    final static String FACET_MINLENGTH         = "minLength";
    final static String FACET_MAXLENGTH         = "maxLength";
    final static String FACET_PATTERN           = "pattern";
    final static String FACET_ENUMERATION       = "enumeration";
    final static String FACET_TOTALDIGITS       = "totalDigits";
    final static String FACET_FRACTIONDIGITS    = "fractionDigits";
    final static String FACET_MININCLUSIVE      = "minInclusive";
    final static String FACET_MAXINCLUSIVE      = "maxInclusive";
    final static String FACET_MINEXCLUSIVE      = "minExclusive";
    final static String FACET_MAXEXCLUSIVE      = "maxExclusive";
    final static String FACET_WHITESPACE        = "whiteSpace";
}
