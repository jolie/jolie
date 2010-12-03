package joliex.wsdl.validation;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

// JAXP
import javax.xml.XMLConstants;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

// DOM
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;

public class TestDOMValidation {

	/*
org.xml.sax.SAXParseException: s4s-elt-character: 
	 Non-whitespace characters are not allowed in schema elements other than 'xs:appinfo' and 'xs:documentation'.
	 Saw 'This type is extended by  component types
         to allow attributes from other namespaces to be added.'.
	 */

    public static void main(String[] args) {
        try {
//            if (args.length != 2) {
//                System.err.println ("Usage: java TestDOMValidation " +
//                                    "[xml filename] [schema filename]");
//                System.exit (1);
//            }
            String schFileName = "./WSDL11.xsd.xml";
            String docFileName = "./provaInputPortsTrial00.wsdl";
			//String docFileName = "./provaTypes.wsdl";

            // Get Document Builder Factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Leave off validation, and turn off namespaces
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            ErrorHandler eh = new SimpleErrorHandler();
            builder.setErrorHandler(eh);
            //---------------------------
            File f_doc = new File(docFileName);
			if (!f_doc.exists()) {
			System.err.println(" file NON trovato docFileName=" + docFileName);
			}
            System.out.println(" file caricato docFileName=" + f_doc);
            Document doc = builder.parse(f_doc);

            System.out.println(" doc well-formed=" + doc+" =================================================");
            printNode(doc,"-");
			//File f_doc=new File(args[0]);
            //------------ well format validation for schema
			File f_sch = new File(schFileName);
			if (!f_sch.exists()) {
			System.err.println(" file NON trovato docFileName=" + docFileName);
			}
            System.out.println(" file caricato docFileName=" + f_sch);
            Document sch_as_xml = builder.parse(f_sch);
            System.out.println(" sch well-formed (as basic xml) =" + sch_as_xml+" =================================================");
			 printNode(sch_as_xml,"-");
            //File f_doc=new File(args[0]);
			//-------------
            // Handle validation
            SchemaFactory constraintFactory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            //File f_sch = new File(schFileName);
            System.out.println(" file caricato schFileName=" + f_sch);
			//TODO AGGIUNGERE Altri files che importano httpBinding etc
            Source constraints = new StreamSource(f_sch);
			//Source[] schemas={}
            Schema schema = constraintFactory.newSchema(constraints);
            Validator validator = schema.newValidator();

            // Validate the DOM tree of the document
			DOMResult domRes=new DOMResult();
            try {
                validator.validate(new DOMSource(doc),domRes);

                System.out.println("Document validates fine with result="+domRes);
            } catch (org.xml.sax.SAXException e) {
                System.out.println("Validation error: " + e.getMessage());
            }

        } catch (ParserConfigurationException e) {
            System.err.println("The underlying parser does not support the requested features.");
        } catch (FactoryConfigurationError e) {
            System.err.println("Error occurred obtaining Document Builder Factory.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printNode(Node node, String indent) {
        switch (node.getNodeType()) {
            case Node.DOCUMENT_NODE:
                System.out.println("<xml version=\"1.0\">\n");
                // recurse on each child
                NodeList nodes = node.getChildNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        printNode(nodes.item(i), "");
                    }
                }
                break;

            case Node.ELEMENT_NODE:
                String name = node.getNodeName();
                System.out.print(indent + "<" + name);
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node current = attributes.item(i);
                    System.out.print(" " + current.getNodeName()
                            + "=\"" + current.getNodeValue()
                            + "\"");
                }
                System.out.print(">");

                // recurse on each child
                NodeList children = node.getChildNodes();
                if (children != null) {
                    for (int i = 0; i < children.getLength(); i++) {
                        printNode(children.item(i), indent + "  ");
                    }
                }

                System.out.print("</" + name + ">");
                break;

            case Node.TEXT_NODE:
                System.out.print(node.getNodeValue());
                break;
        }
    }
}
