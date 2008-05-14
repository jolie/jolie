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

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

/**
 * Used to parse &lt;xs:annotation>.
 * 
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public abstract class AnnotationParser {
    /**
     * Called every time a new &lt;xs:annotation> element
     * is found.
     * 
     * The sub-tree rooted at &lt;xs:annotation> will be
     * sent to this ContentHandler as if it is a whole document.
     * 
     * @param context
     *      indicates the schema component that owns this annotation.
     *      Always non-null.
     * @param parentElementName
     *      local name of the element that contains &lt;xs:annotation>.
     *      (e.g., "element", "attribute", ... )
     * @param errorHandler
     *      The error handler that the client application specifies.
     *      The returned content handler can send its errors to this
     *      object.
     * @param entityResolver
     *      The entity resolver that is currently in use. Again,
     *      The returned content handler can use this object
     *      if it needs to resolve entities.
     */
    public abstract ContentHandler getContentHandler(
        AnnotationContext context,
        String parentElementName,
        ErrorHandler errorHandler,
        EntityResolver entityResolver );
    
    /**
     * Once the SAX events are fed to the ContentHandler,
     * this method will be called to retrieve the parsed result.
     * 
     * @param existing
     *      An annotation object which was returned from another
     *      AnnotationParser before. Sometimes, one schema component
     *      can have multiple &lt:xs:annotation> elements and
     *      this parameter is used to merge all those annotations
     *      together. If there is no existing object, null will be
     *      passed.
     * @return
     *      Any object, including null.
     */
    public abstract Object getResult( Object existing );
}

