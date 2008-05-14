package com.sun.xml.xsom.util;

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.parser.AnnotationContext;
import com.sun.xml.xsom.parser.AnnotationParser;
import com.sun.xml.xsom.parser.AnnotationParserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

/**
 * {@link AnnotationParserFactory} that parses annotations into a W3C DOM.
 *
 * <p>
 * If you use this parser factory, you'll get {@link Element} that represents
 * &lt;xs:annotation> from {@link XSAnnotation#getAnnotation()}.
 *
 * <p>
 * When multiple &lt;xs:annotation>s are found for the given schema component,
 * you'll see all &lt;xs:appinfo>s and &lt;xs:documentation>s combined under
 * one &lt;xs:annotation> element.
 *
 * @author Kohsuke Kawaguchi
 */
public class DomAnnotationParserFactory implements AnnotationParserFactory {
    public AnnotationParser create() {
        return new AnnotationParserImpl();
    }

    private static final SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    private static class AnnotationParserImpl extends AnnotationParser {

        /**
         * Identity transformer used to parse SAX into DOM.
         */
        private final TransformerHandler transformer;
        private DOMResult result;

        AnnotationParserImpl() {
            try {
                transformer = stf.newTransformerHandler();
            } catch (TransformerConfigurationException e) {
                throw new Error(e); // impossible
            }
        }

        public ContentHandler getContentHandler(AnnotationContext context, String parentElementName, ErrorHandler errorHandler, EntityResolver entityResolver) {
            result = new DOMResult();
            transformer.setResult(result);
            return transformer;
        }

        public Object getResult(Object existing) {
            Document dom = (Document)result.getNode();
            Element e = dom.getDocumentElement();
            if(existing instanceof Element) {
                // merge all the children
                Element prev = (Element) existing;
                Node anchor = e.getFirstChild();
                while(prev.getFirstChild()!=null) {
                    Node move = prev.getFirstChild();
                    e.insertBefore(e.getOwnerDocument().adoptNode(move), anchor );
                }
            }
            return e;
        }
    }
}
