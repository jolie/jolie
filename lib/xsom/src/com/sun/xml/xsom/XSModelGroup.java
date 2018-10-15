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
 * Model group.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSModelGroup extends XSComponent, XSTerm, Iterable<XSParticle>
{
    /**
     * Type-safe enumeration for kind of model groups.
     * Constants are defined in the {@link XSModelGroup} interface.
     */
    public static enum Compositor {
        ALL("all"),CHOICE("choice"),SEQUENCE("sequence");

        private Compositor(String _value) {
            this.value = _value;
        }

        private final String value;
        /**
         * Returns the human-readable compositor name.
         * 
         * @return
         *      Either "all", "sequence", or "choice".
         */
        public String toString() {
            return value;
        }
    }
    /**
     * A constant that represents "all" compositor.
     */
    static final Compositor ALL = Compositor.ALL;
    /**
     * A constant that represents "sequence" compositor.
     */
    static final Compositor SEQUENCE = Compositor.SEQUENCE;
    /**
     * A constant that represents "choice" compositor.
     */
    static final Compositor CHOICE = Compositor.CHOICE;

    Compositor getCompositor();

    /**
     * Gets <i>i</i>-ith child.
     */
    XSParticle getChild(int idx);
    /**
     * Gets the number of children.
     */
    int getSize();

    /**
     * Gets all the children in one array.
     */
    XSParticle[] getChildren();
}
