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
package com.sun.xml.xsom.impl.parser;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Manages patchers.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface PatcherManager {
    void addPatcher( Patch p );
    /**
     * Reports an error during the parsing.
     * 
     * @param source
     *      location of the error in the source file, or null if
     *      it's unavailable.
     */
    void reportError( String message, Locator source ) throws SAXException;
    
    
    public interface Patcher {
        void run() throws SAXException;
    }
}
