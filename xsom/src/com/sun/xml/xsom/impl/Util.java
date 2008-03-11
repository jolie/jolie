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

import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
class Util {
    private static XSType[] listDirectSubstitutables( XSType _this ) {
        ArrayList r = new ArrayList();
        
        // TODO: handle @block
        Iterator itr = ((SchemaImpl)_this.getOwnerSchema()).parent.iterateTypes();
        while( itr.hasNext() ) {
            XSType t = (XSType)itr.next();
            if( t.getBaseType()==_this )
                r.add(t);
        }
        return (XSType[]) r.toArray(new XSType[r.size()]);
    }

    public static XSType[] listSubstitutables( XSType _this ) {
        Set substitables = new HashSet();
        buildSubstitutables( _this, substitables );
        return (XSType[]) substitables.toArray(new XSType[substitables.size()]);
    }
    
    public static void buildSubstitutables( XSType _this, Set substitutables ) {
        if( _this.isLocal() )    return;
        buildSubstitutables( _this, _this, substitutables );
    }
    
    private static void buildSubstitutables( XSType head, XSType _this, Set substitutables ) {
        if(!isSubstitutable(head,_this))
            return;    // no derived type of _this can substitute head.
        
        if(substitutables.add(_this)) {
            XSType[] child = listDirectSubstitutables(_this);
            for( int i=0; i<child.length; i++ )
                buildSubstitutables( head, child[i], substitutables );
        }
    }
    
    /**
     * Implements
     * <code>Validation Rule: Schema-Validity Assessment (Element) 1.2.1.2.4</code> 
     */
    private static boolean isSubstitutable( XSType _base, XSType derived ) {
        // too ugly to the point that it's almost unbearable.
        // I mean, it's not even transitive. Thus we end up calling this method
        // for each candidate
        if( _base.isComplexType() ) {
            XSComplexType base = _base.asComplexType();
            
            for( ; base!=derived; derived=derived.getBaseType() ) {
                if( base.isSubstitutionProhibited( derived.getDerivationMethod() ) )
                    return false;    // Type Derivation OK (Complex)-1
            }
            return true;
        } else {
            // simple type don't have any @block
            return true;
        }
    }

}
