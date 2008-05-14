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
package com.sun.xml.xsom.impl;

import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSIdentityConstraint;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;

/**
 * Reference to other schema components.
 * 
 * <p>
 * There are mainly two different types of references. One is
 * the direct reference, which is only possible when schema components
 * are already available when references are made.
 * The other is the lazy reference, which keeps references by names
 * and later look for the component by name.
 * 
 * <p>
 * This class defines interfaces that define the behavior of such
 * references and classes that implement direct reference semantics.
 * 
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public abstract class Ref {
    
    public static interface Term {
        /** Obtains a reference as a term. */
        XSTerm getTerm();
    }

    public static interface Type {
        /** Obtains a reference as a type. */
        XSType getType();
    }

    public static interface ContentType {
        XSContentType getContentType();
    }
    
    public static interface SimpleType extends Ref.Type {
        public XSSimpleType getType();
    }
    
    public static interface ComplexType extends Ref.Type {
        public XSComplexType getType();
    }
    
    public static interface Attribute {
        XSAttributeDecl getAttribute();
    }

    public static interface AttGroup {
        XSAttGroupDecl get();
    }
    
    public static interface Element extends Term {
        XSElementDecl get();
    }

    public static interface IdentityConstraint {
        XSIdentityConstraint get();
    }
//
//    
//    private static void _assert( boolean b ) {
//        if(!b)
//            throw new InternalError("assertion failed");
//    }
}
