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

import com.sun.xml.xsom.visitor.XSTermFunction;
import com.sun.xml.xsom.visitor.XSTermVisitor;
import com.sun.xml.xsom.visitor.XSTermFunctionWithParam;

/**
 * A component that can be referenced from {@link XSParticle}
 * 
 * This interface provides a set of type check functions (<code>isXXX</code>),
 * which are essentially:
 * 
 * <pre>
 * boolean isXXX() {
 *     return this instanceof XXX;
 * }
 * <pre>
 * 
 * and a set of cast functions (<code>asXXX</code>), which are
 * essentially:
 * 
 * <pre>
 * XXX asXXX() {
 *     if(isXXX())  return (XXX)this;
 *     else          return null;
 * }
 * </pre>
 */
public interface XSTerm extends XSComponent
{
    void visit( XSTermVisitor visitor );
    <T> T apply( XSTermFunction<T> function );
    <T,P> T apply( XSTermFunctionWithParam<T,P> function, P param );

    // cast functions
    boolean isWildcard();
    boolean isModelGroupDecl();
    boolean isModelGroup();
    boolean isElementDecl();

    XSWildcard asWildcard();
    XSModelGroupDecl asModelGroupDecl();
    XSModelGroup asModelGroup();
    XSElementDecl asElementDecl();
}
