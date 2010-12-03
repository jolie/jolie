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

public class TestDOMValidationViaSchemaCodeDriven {

    public static void validate(String schemaFileName, String docfileName) {
        try {
//            if (args.length != 2) {
//                System.err.println ("Usage: java TestDOMValidation " +
//                                    "[xml filename] [schema filename]");
//                System.exit (1);
//            }
            File f_sch = new File(schemaFileName);
//-----------------------------------------------------------
            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            
            Source schemasource = new StreamSource(schemaFileName);
             System.out.println("schemasource=" + schemasource);
            //Schema sch = schemaFactory.newSchema(new Source[]{schemasource});
             Schema sch = schemaFactory.newSchema(schemasource);
            System.out.println("sch=" + sch);
//------------------------------------------------------------------
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setSchema(sch);
            // Leave off validation, and turn off namespaces
            factory.setValidating(false);
            factory.setNamespaceAware(true);
            System.out.println("factory=" + factory);

            DocumentBuilder builder = factory.newDocumentBuilder();

            //-----------------------------------------------------------
            ErrorHandler eh = new SimpleErrorHandler();
            builder.setErrorHandler(eh);
            //builder.setEntityResolver(null)
            //---------------------------
            File f_doc = new File(docfileName);
            Document doc = builder.parse(f_doc);
            System.out.println("Document parsed fine.");
            //File f_doc=new File(args[0]);

            // Handle validation
            SchemaFactory constraintFactory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            f_sch = new File(schemaFileName);
            Source constraints = new StreamSource(f_sch);
            Schema schema = constraintFactory.newSchema(constraints);
            Validator validator = schema.newValidator();

            // Validate the DOM tree
            try {
                validator.validate(new DOMSource(doc));
                System.out.println("Document validates fine.");
            } catch (org.xml.sax.SAXException e) {
                System.out.println("Validation error: " + e.getMessage());
            }

        } catch (ParserConfigurationException e) {
            System.out.println("The underlying parser does not support the requested features.");
        } catch (FactoryConfigurationError e) {
            System.out.println("Error occurred obtaining Document Builder Factory.");
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

    public static void main(String[] args) {
        String sch = "./WSDL11.xsd.xml";
        String doc = "./provaInputPortsTrial00.wsdl";
        validate(sch, doc);
    }
}
