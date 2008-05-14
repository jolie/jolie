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
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import com.sun.xml.xsom.impl.parser.Messages;

/**
 * Standard XMLParser implemented by using JAXP.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class JAXPParser implements XMLParser {
    
    private final SAXParserFactory factory;
    
    public JAXPParser( SAXParserFactory factory ) {
        factory.setNamespaceAware(true);    // just in case
        this.factory = factory;
    }
    
    public JAXPParser() {
        this( SAXParserFactory.newInstance());
    }
    


    
    


    public void parse( InputSource source, ContentHandler handler,
        ErrorHandler errorHandler, EntityResolver entityResolver )
        
        throws SAXException, IOException {
        
        try {
            XMLReader reader = factory.newSAXParser().getXMLReader();
            reader = new XMLReaderEx(reader);
            
            reader.setContentHandler(handler);
            if(errorHandler!=null)
                reader.setErrorHandler(errorHandler);
            if(entityResolver!=null)
                reader.setEntityResolver(entityResolver);
            reader.parse(source);
        } catch( ParserConfigurationException e ) {
            // in practice this won't happen
            SAXParseException spe = new SAXParseException(e.getMessage(),null,e);
            errorHandler.fatalError(spe);
            throw spe;
        }
    }



    /**
     * XMLReader with improved error message for entity resolution failure.
     * 
     * TODO: this class is completely stand-alone, so it shouldn't be
     * an inner class.
     */
    private static class XMLReaderEx extends XMLFilterImpl {
        
        private Locator locator;
        
        XMLReaderEx( XMLReader parent ) {
            this.setParent(parent);
        }
        
        /**
         * Resolves entities and reports user-friendly error messages.
         * 
         * <p>
         * Some XML parser (at least Xerces) does not report much information
         * when it fails to resolve an entity, which is often quite
         * frustrating. For example, if you are behind a firewall and the 
         * schema contains a reference to www.w3.org, and there is no
         * entity resolver, the parser will just throw an IOException
         * that doesn't contain any information about where that reference
         * occurs nor what it is accessing.
         * 
         * <p>
         * By implementing an EntityResolver and resolving the reference
         * by ourselves, we can report an error message with all the
         * necessary information to fix the problem.
         * 
         * <p>
         * Note that we still need to the client-specified entity resolver
         * to let the application handle entity resolution. Here we just catch
         * an IOException and add more information.
         */
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
            try {
                InputSource is=null;
                
                // ask the client-specified entity resolver first
                if( this.getEntityResolver()!=null)  
                    is = this.getEntityResolver().resolveEntity(publicId,systemId);
                if( is!=null )  return is;  // if that succeeds, fine.
                
                // rather than returning null, resolve it now
                // so that we can detect errors.
                is = new InputSource( new URL(systemId).openStream() );
                is.setSystemId(systemId);
                is.setPublicId(publicId);
                return is;
            } catch( IOException e ) {
                // catch this error and provide a nice error message, rather than
                // just throwing this IOException.
                SAXParseException spe = new SAXParseException(
                    Messages.format(Messages.ERR_ENTITY_RESOLUTION_FAILURE,
                        systemId, e.toString()),    // use the toString method to get the class name
                    locator, e );
                if(this.getErrorHandler()!=null)
                    this.getErrorHandler().fatalError(spe);
                throw spe;
            }
        }
        
        public void setDocumentLocator(Locator locator) {
            super.setDocumentLocator(locator);
            this.locator = locator;
        }
    }
}
