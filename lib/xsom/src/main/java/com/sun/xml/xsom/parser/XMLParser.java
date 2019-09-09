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
package com.sun.xml.xsom.parser;

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Interface that hides the detail of parsing mechanism.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XMLParser {
    /**
     * Parses the document identified by the given input source
     * and sends SAX events to the given content handler.
     * 
     * <p>
     * This method must be re-entrant.
     * 
     * @param errorHandler
     *      Errors found during the parsing must be reported to
     *      this handler so that XSOM can recognize that something went wrong.
     *      Always a non-null valid object
     * @param entityResolver
     *      Entity resolution should be done through this interface.
     *      Can be null.
     * 
     * @exception SAXException
     *      If ErrorHandler throws a SAXException, this method
     *      will tunnel it to the caller. All the other errors
     *      must be reported to the error handler.
     */
    void parse( InputSource source, ContentHandler handler,
        ErrorHandler errorHandler, EntityResolver entityResolver )
        
        throws SAXException, IOException;
}
