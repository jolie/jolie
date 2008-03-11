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
package com.sun.xml.xsom.impl.util;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class ResourceEntityResolver implements EntityResolver {
    public ResourceEntityResolver( Class<?> _base ) {
        this.base = _base;
    }
    
    private final Class<?> base;
    
    public InputSource resolveEntity( String publicId, String systemId ) {
        return new InputSource(base.getResourceAsStream(systemId));
    }
}
