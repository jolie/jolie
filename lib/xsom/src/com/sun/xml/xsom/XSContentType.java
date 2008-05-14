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

import com.sun.xml.xsom.visitor.XSContentTypeFunction;
import com.sun.xml.xsom.visitor.XSContentTypeVisitor;

/**
 * Content of a complex type.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSContentType extends XSComponent
{
    /**
     * Equivalent of <code>(this instanceof XSSimpleType)?this:null</code>
     */
    XSSimpleType asSimpleType();
    /**
     * Equivalent of <code>(this instanceof XSParticle)?this:null</code>
     */
    XSParticle asParticle();
    /**
     * If this content type represents the empty content, return <code>this</code>,
     * otherwise null.
     */
    XSContentType asEmpty();

    <T> T apply( XSContentTypeFunction<T> function );
    void visit( XSContentTypeVisitor visitor );
}
