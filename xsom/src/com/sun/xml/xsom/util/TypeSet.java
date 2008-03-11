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

import com.sun.xml.xsom.XSType;

/**
 * A simple abstraction for a set of Types that defines containment functions.
 * 
 * @author <a href="mailto:Ryan.Shoemaker@Sun.COM">Ryan Shoemaker</a>, Sun Microsystems, Inc.
 */
public abstract class TypeSet {

    /**
     * Return true if this TypeSet contains the specified type.
     * 
     * Concrete implementations of this method determine what it
     * means for the TypeSet to "contain" a type.
     *  
     * @param type the type
     * @return true iff this TypeSet contains the specified type
     */
    public abstract boolean contains(XSType type);

    /**
     * Calculate the TypeSet formed by the intersection of two
     * other TypeSet objects.
     * 
     * @param a a TypeSet
     * @param b another TypeSet
     * @return the intersection of a and b
     */
    public static TypeSet intersection(final TypeSet a, final TypeSet b) {
        return new TypeSet(){
            public boolean contains(XSType type) {
                return a.contains(type) && b.contains(type);
            }
        };
    }

    /**
     * Calculate the TypeSet formed by the union of two
     * other TypeSet objects.
     * 
     * @param a a TypeSet
     * @param b another TypeSet
     * @return the union of a and b
     */
    public static TypeSet union(final TypeSet a, final TypeSet b) {
        return new TypeSet(){
            public boolean contains(XSType type) {
                return a.contains(type) || b.contains(type);
            }
        };
    }
}
