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
 * Base interface of all "declarations".
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSDeclaration extends XSComponent
{
    /**
     * Target namespace to which this component belongs.
     * <code>""</code> is used to represent the default no namespace.
     */
    String getTargetNamespace();

    /**
     * Gets the (local) name of the declaration.
     *
     * @return null if this component is anonymous.
     */
    String getName();

    /**
     * @deprecated use the isGlobal method, which always returns
     * the opposite of this function. Or the isLocal method.
     */
    boolean isAnonymous();

    /**
     * Returns true if this declaration is a global declaration.
     * 
     * Global declarations are those declaration that can be enumerated
     * through the schema object.
     */
    boolean isGlobal();

    /**
     * Returns true if this declaration is a local declaration.
     * Equivalent of <code>!isGlobal()</code>
     */
    boolean isLocal();
}
