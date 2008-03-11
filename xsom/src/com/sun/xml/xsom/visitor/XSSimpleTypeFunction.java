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
package com.sun.xml.xsom.visitor;

import com.sun.xml.xsom.XSListSimpleType;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSUnionSimpleType;

/**
 * Function object that works on {@link com.sun.xml.xsom.XSSimpleType}
 * and its derived interfaces.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke,kawaguchi@sun.com)
 */
public interface XSSimpleTypeFunction<T> {
    T listSimpleType( XSListSimpleType type );
    T unionSimpleType( XSUnionSimpleType type );
    T restrictionSimpleType( XSRestrictionSimpleType type );
}

