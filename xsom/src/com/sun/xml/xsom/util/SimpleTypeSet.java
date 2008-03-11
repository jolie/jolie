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

import java.util.Set;

import com.sun.xml.xsom.XSType;

/**
 * A very simple TypeSet.
 * 
 * The contains method returns true iff the set explicitly contains an
 * instance of the specified XSType.
 * 
 * @author <a href="mailto:Ryan.Shoemaker@Sun.COM">Ryan Shoemaker</a>, Sun Microsystems, Inc.
 */
public class SimpleTypeSet extends TypeSet {

    private final Set typeSet;
    
    public SimpleTypeSet(Set s) {
        typeSet = s;
    }
    
    /* (non-Javadoc)
     * @see com.sun.xml.xsom.util.TypeSet#contains(com.sun.xml.xsom.XSDeclaration)
     */
    public boolean contains(XSType type) {
        return typeSet.contains(type);
    }

}
