/*
 * This class generates a fixed wsdl1.1 document using WSDL4J API;
 * The main purpose is to test WSDL4J API.
 *
 */
package joliex.wsdl;

import joliex.wsdl.*;
import com.ibm.wsdl.BindingImpl;
import com.ibm.wsdl.BindingInputImpl;
import com.ibm.wsdl.BindingOperationImpl;
import com.ibm.wsdl.BindingOutputImpl;
import com.ibm.wsdl.DefinitionImpl;
import com.ibm.wsdl.FaultImpl;
import com.ibm.wsdl.InputImpl;
import com.ibm.wsdl.MessageImpl;
import com.ibm.wsdl.OutputImpl;
import com.ibm.wsdl.PartImpl;
import com.ibm.wsdl.PortTypeImpl;
import com.ibm.wsdl.ServiceImpl;
import com.ibm.wsdl.TypesImpl;
import com.ibm.wsdl.extensions.schema.SchemaImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;
import com.ibm.wsdl.util.xml.QNameUtils;
import com.sun.org.apache.xerces.internal.dom.DocumentTypeImpl;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Francesco
 * see also http://forums.sun.com/thread.jspa?threadID=670586
 */
public class WsdlEffectorImplWsdl4J {

    //static final String tns = "http://www.italianasoftware.com/wsdl/FirstServiceByWSDL4J";//.wsdl
    static final String tns = "http://www.italianasoftware.com/";
    static final String schema = "http://www.w3.org/2001/XMLSchema";
    static final String soap = "http://schemas.xmlsoap.org/wsdl/soap/";
    static final String wsdl = "http://schemas.xmlsoap.org/wsdl/";
    static final String soapOverHttp = "http://schemas.xmlsoap.org/wsdl/soap/http";
    static final String tns_prefix = "tns";
    static final String schema_prefix = "xs";
    static final String soap_prefix = "soap";
    static final String wsdl_prefix = "wsdl";
    //static String soapOverHttp_prefix = "http://schemas.xmlsoap.org/wsdl/soap/http";
    static ExtensionRegistry extensionRegistry;
    private static WSDLFactory wsdlFactory;
    private static Definition def;
    /*
    public static Definition init() {
    try {
    wsdlFactory = WSDLFactory.newInstance();
    def = wsdlFactory.newDefinition();
    extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
    WSDLWriter writer = wsdlFactory.newWSDLWriter();

    //Veder se prendere def.createBlaBla
    //TODO ...spostare quete vars
    String serviceName = "TwiceService";
    String opName = "twice";
    QName servDefQN = new QName(serviceName + "QN_TODO_rendereParametrico");
    def.setQName(servDefQN);

    String targetNS = tns;

    def.addNamespace("soap", soap);
    def.addNamespace("tns", tns);
    def.addNamespace("schema", schema);
    def.setTargetNamespace(targetNS);
    } catch (WSDLException ex) {
    Logger.getLogger(WsdlEffectorImplWsdl4J.class.getName()).log(Level.SEVERE, null, ex);
    }
    return def;
    }

    public static void addMessage(String inputPartName, String inputPartType) {
    Input input = def.createInput();
    Message inputMessage = def.createMessage();
    Part inputPart = def.createPart();
    inputPart.setName(inputPartName);
    inputPart.setTypeName(new QName("http://www.w3.org/2001/XMLSchema", inputPartType));
    inputMessage.addPart(inputPart);
    input.setMessage(inputMessage);

    Output output = def.createOutput();
    Message outputMessage = def.createMessage();

    output.setMessage(outputMessage);
    }

    public static void addPortType(String opName) {
    Operation operation = def.createOperation();
    operation.setName(opName);
    operation.setStyle(OperationType.REQUEST_RESPONSE);

    Input input = def.createInput();
    Message inputMessage = def.createMessage();
    Part inputPart = def.createPart();
    inputPart.setName("string");
    inputPart.setTypeName(new QName("http://www.w3.org/2001/XMLSchema", "string"));
    inputMessage.addPart(inputPart);
    operation.setInput(input);
    input.setMessage(inputMessage);
    Output output = def.createOutput();
    Message outputMessage = def.createMessage();
    operation.setOutput(output);
    output.setMessage(outputMessage);

    }

    public static void addBinding(PortType pt, Operation wsdlOp) {
    Binding b = def.createBinding();
    //def.a
    b.setUndefined(false);
    QName binding_QN = new QName("Binding_QN");
    b.setQName(binding_QN);
    b.setPortType(pt);

    BindingOperation bOp = new BindingOperationImpl();

    bOp.setOperation(wsdlOp);
    //SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension(BindingOperation.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "operation"));
    SOAPOperation soapOperation = new SOAPOperationImpl();
    soapOperation.setStyle("document");
    // soapOperation.setStyle("rpc");
    //QName soapOpQN=new QName("")

    BindingInput bi = new BindingInputImpl();
    bi.setName("bindingInput_name_str");
    bOp.setBindingInput(bi);
    BindingOutput bo = new BindingOutputImpl();
    bo.setName("bindingOutput_name_str");
    bOp.setBindingOutput(bo);
    //bOp.setBindingInput(null)
    //bOp.setBindingOutput(null)
    b.addBindingOperation(bOp);
    def.addBinding(b);
    }

    public static void addBindingSOAP(PortType pt, Operation wsdlOp) {
    //TODO
    Binding b = def.createBinding();
    //def.a
    b.setUndefined(false);
    QName binding_QN = new QName("Binding_QN");
    b.setQName(binding_QN);
    b.setPortType(pt);

    BindingOperation bOp = new BindingOperationImpl();

    bOp.setOperation(wsdlOp);
    //SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension(BindingOperation.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "operation"));
    SOAPOperation soapOperation = new SOAPOperationImpl();
    soapOperation.setStyle("document");
    // soapOperation.setStyle("rpc");
    //QName soapOpQN=new QName("")

    BindingInput bi = new BindingInputImpl();
    bi.setName("bindingInput_name_str");
    bOp.setBindingInput(bi);
    BindingOutput bo = new BindingOutputImpl();
    bo.setName("bindingOutput_name_str");
    bOp.setBindingOutput(bo);
    //bOp.setBindingInput(null)
    //bOp.setBindingOutput(null)
    b.addBindingOperation(bOp);
    def.addBinding(b);
    }

    public static void addService(Port p) {
    Service s = new ServiceImpl();
    QName qn0 = new QName("Service_QN");
    // javax.xml.namespace.QName qn=new QNameUtils().newQName( );
    s.setQName(qn0);
    s.addPort(p);
    }
     */
    /*
    public static void test01() {
    String serviceName = "order_service";
    String opName = "create_order";

    Definition def = init();

    // try {

    //xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"

    Operation operation = def.createOperation();
    operation.setName(opName);
    operation.setStyle(OperationType.REQUEST_RESPONSE);

    Input input = def.createInput();
    Message inputMessage = def.createMessage();
    Part inputPart = def.createPart();
    inputPart.setName("string");
    inputPart.setTypeName(new QName(schema, "string"));
    inputMessage.addPart(inputPart);
    operation.setInput(input);
    input.setMessage(inputMessage);
    Output output = def.createOutput();


    Message outputMessage = def.createMessage();
    operation.setOutput(output);
    output.setMessage(outputMessage);

    BindingOperation bindOp = def.createBindingOperation();
    bindOp.setName(opName);

    //SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension(BindingOperation.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "operation"));
    SOAPOperation soapOperation = new SOAPOperationImpl();
    soapOperation.setStyle("document");
    // soapOperation.setStyle("rpc");
    //soapOperation.set
    soapOperation.setSoapActionURI("");
    bindOp.addExtensibilityElement(soapOperation);
    bindOp.setOperation(operation);

    BindingInput bindingInput = def.createBindingInput();

    //SOAPBody inputBody = (SOAPBody) extensionRegistry.createExtension(BindingInput.class, new QName("http://schemas.xmlsoap.org/wsdl/soap/", "body"));
    SOAPBody inputBody = new SOAPBodyImpl();
    inputBody.setUse("literal");
    //inputBody.setEncodingStyles(null);
    //inputBody.setUse("encoded");
    bindingInput.addExtensibilityElement(inputBody);
    bindOp.setBindingInput(bindingInput);
    BindingOutput bindingOutput = def.createBindingOutput();
    bindingOutput.addExtensibilityElement(inputBody);
    bindOp.setBindingOutput(bindingOutput);
    //        } catch (WSDLException ex) {
    //            Logger.getLogger(WsdlEffectorImplWsdl4J.class.getName()).log(Level.SEVERE, null, ex);
    //        }
    }
     */

    public static void test00(String fileName) {
        //if (fileName.equals("")) {fileName="./src/test/resources/prova_out.wsdl";}
        Definition rr;
        Schema sch;
        String subTypeNameReq = "num";
        String subTypeNameResp = "result";
        //Definition def=init();
        {
            Writer fw = null;
            try {
                WSDLFactory f = WSDLFactory.newInstance();
                WSDLReader r = f.newWSDLReader();
                //r.setExtensionRegistry(null);
                r.setFeature("javax.wsdl.verbose", true);
                r.setFeature("javax.wsdl.importDocuments", true);
                //r.readWSDL("");
                WSDLWriter ww = f.newWSDLWriter();

                fw = new FileWriter(fileName);
                //def = new DefinitionImpl();

                wsdlFactory = WSDLFactory.newInstance();
                def = wsdlFactory.newDefinition();
                extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
                //extensionRegistry.
                //TODO ...spostare quete vars
                String serviceName = "TwiceService";
                String opName = "twice";

                QName servDefQN = new QName(serviceName);
                def.setQName(servDefQN);

                String targetNS = tns;

                def.addNamespace("wsdl", wsdl);
                def.addNamespace("soap", soap);
                def.addNamespace("tns", tns);
                def.addNamespace("xs", schema);
                def.setTargetNamespace(targetNS);

                //def.setDocumentBaseURI("http://docBaseURI");
//============================= TYPES =============================
                //Types types=new TypesImpl();
                Types types = def.createTypes();
                //TODO Come aggiungere schema (o riferimento a schema)  per i tipi?

                //sch = new SchemaImpl();
                Schema typesExt = (Schema) extensionRegistry.createExtension(Types.class, new QName(schema, "schema"));
                //typesExt.
                /*TODO DeCommentare la seguente parte di codice e vedere perchè non genera l'elemento wsdl:types*/
                //TODO Da qui in poi devo solo riempire lo schema con i giusti elementi...;
                //TODO Come creare elementi da settare nelle istanze della classe SchemaImpl
                Document document, document02 = null;
                Element rootElement = null;
                DocumentBuilder db = null;
                try {
                    /*
                    DOMImplementation DOMImplementation =
                    builder.getDOMImplementation();
                    Document manifestAsDOM = DOMImplementation.createDocument(
                    "http://www.somecompany.com/2005/xyz",
                    BuildConstants.MANIFEST_ROOT_NODE_NAME,
                    null);
                    Element root = manifestAsDOM.getDocumentElement();
                    root.setPrefix("xyz");
                    root.setAttribute(
                    "xmlns: xyz",
                    "http://www.somecompany.com/2005/xyz");
                     */
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    System.out.println(" dbf=" + dbf);
                    //dbf.setFeature(opName, true)
                    dbf.setNamespaceAware(true);
                    //dbf.setValidating(true);
                    db = dbf.newDocumentBuilder();
                    System.out.println(" db=" + db);
                    //rootElement.setAttribute("caption", caption);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                document = db.newDocument();
                System.out.println(" document=" + document);
                //DOMImplementation di=db.getDOMImplementation();
                //DocumentType dt=DocumentType();
                //document02=di.createDocument(fileName, serviceName, dt);

                //TODO veridicare le 3 linee seguenti (e le ulteriori linee)
                rootElement = document.createElement("xs:schema");

                rootElement.setAttribute("xmlns:xs",schema);
                //rootElement.setAttribute("xmlns:tns",tns);
                rootElement.setAttribute("targetNamespace",tns);
                //rootElement.setAttribute("targetNamespace:tns",tns);
                //rootElement.setAttributeNS(schema, "xmlns", schema);

                //rootElement.setAttributeNS(tns, "targetNamespace", tns);
                //rootElement.setPrefix("xs");
                //rootElement = document.createElementNS(schema,"schema");
                //TODO Usare (se esiste) un modo migliore per settare il NAMESPACE XMLSchema nei files schema e assicurarsi un corrispondente settaggio nel wsdl
                //TODO Usare un modo migliore per settare il targetNameSpace (tns) negli schema e assicurarsi un corrispondente settaggio IN TUTTO il wsdl,
                //tns:opName    tns:***

                Element elementReq = document.createElement("xs:element");
                //elementReq.setNodeValue("nodeValue");
                elementReq.setAttribute("name", opName);

                Element cTypeReq = document.createElement("xs:complexType");
                //cTypeReq..setAttibute( );
                 Element sequenceReq = document.createElement("xs:sequence");
                 cTypeReq.appendChild(sequenceReq);
                 //sequenceReq.setAttibute( );
                 sequenceReq.setAttribute("name", subTypeNameReq);
                elementReq.appendChild(cTypeReq);
                 

                Element elementResp = document.createElement("xs:element");
                elementResp.setAttribute("name", opName + "Response");
                Element  cTypeResp = document.createElement("xs:complexType");
                Element sequenceResp = document.createElement("xs:sequence");
                 cTypeResp.appendChild(sequenceResp);
                sequenceResp.setAttribute("name", subTypeNameResp);
                elementReq.appendChild(cTypeResp);
                //sch.setElementType(new QName(schema, "xs"));
                //sch.setElement(elementReq);
                //http://www.w3.org/2001/03/14-annotated-WSDL-examples.html
                Element elementFault = document.createElement("xs:element");
                elementFault.setNodeValue("nodeValue");
                elementFault.setAttribute("name", opName + "Fault");

                System.out.println(" document=" + document);
                //elementReq.appendChild(cTypeReq).appendChild(sequenceReq).appendChild(elementResp).appendChild(elementFault);
                rootElement.appendChild(elementReq).appendChild(elementResp);
                //rootElement.appendChild(elementResp);
                //elementReq.setNodeValue("nodeValue");
                //elementReq.setTextContent("textContent");
                //elementReq.setAttribute("name", "contact");

                //System.out.println(" elementReq=" + elementReq);
                //sch.setElement(rootElement);

                //Element schemaElement = new Element();
                //Element schemaElement = ((SchemaImpl) elementReq).getElement();
                //new   org.w3c.dom.Element();

                //NOTA-BENE Punto di aggancio wsdl:type - schema
                //typesExt.setElementType(new QName(schema,"schema"));

                //DOMResult result = new DOMResult();
                //transform(schema.getSource(), result);
                //Document schemaDocument = (Document) rootElement.getNode();
                //return schemaDocument.getDocumentElement();

                typesExt.setElement(rootElement);
                types.addExtensibilityElement(typesExt);

                //types.setDocumentationElement(null);

                System.out.println(" types=" + types);
                def.setTypes(types);
//============================= MESSAGE =============================
                Message msg_req = new MessageImpl();
                msg_req.setUndefined(false);
                QName msg_req_QN = new QName(serviceName + "Request");
                msg_req.setQName(msg_req_QN);
                //msg_req.
                //TODO msg_req.setQName(null);
                Part part01a = new PartImpl();
                //--------------------
                part01a.setName("parameters");
                part01a.setElementName(new QName(opName));
                //----------------

                QName partType01a = new QName(schema, "string");
                //part01a.setTypeName(partType01a);
                msg_req.addPart(part01a);
                def.addMessage(msg_req);
//------------------------- Resp mesg --------------------
                Message msg_resp = new MessageImpl();
                msg_resp.setUndefined(false);
                QName msg_resp_QN = new QName(serviceName + "Response");
                msg_resp.setQName(msg_resp_QN);
                QName qn02a = new QName(opName + "Response");
                //msg_resp.setQName("");
                //QName qn02a=new QName("schema:String");
                Part p02a = new PartImpl();
                //--------------------
                //DOCUMENT
                p02a.setName("parameters");
                p02a.setElementName(qn02a);
                //----------------
                //rpc
                //p02a.setName(serviceName + "ResponsePartName");
                //p02a.setTypeName(qn02a);
                //----------------
                //Node.TEXT_NODE;
                //node= new NodeImpl();
                //QName qn=QNameUtils.newQName(node);
                msg_resp.addPart(p02a);
                def.addMessage(msg_resp);
                System.out.println(" -----------  fine aggiunta messaggi --------------------- ");
//=================================== PORTTYPES (interfaces) ===================================
                //NOTA-BENE i 4 tipi di primitive dicomm sono distitne dalla presenzao meno di elementi input output in questa sezione dentro le operazioni
                Operation wsdlOp = def.createOperation();
                //wsdlOp.setName(serviceName + "FirstOp");
                wsdlOp.setName(opName);
                wsdlOp.setStyle(OperationType.REQUEST_RESPONSE);
                wsdlOp.setUndefined(false);
                //List<> lp=new ArrayList()<Param>;
                //wsdlOp.setParameterOrdering(null);
                Input in = new InputImpl();
                in.setName("inputInName");
                in.setMessage(msg_req);
                wsdlOp.setInput(in);
                Output out = new OutputImpl();
                out.setName("outputOutName");
                out.setMessage(msg_req);
                wsdlOp.setOutput(out);
                Fault fault = new FaultImpl();
                fault.setName("faultName");
                fault.setMessage(msg_resp);
                wsdlOp.addFault(fault);
                /*Input in=new InputImpl();
                //in.
                wsdlOp.setInput(in); */
//-----------------------------------------------------
                PortType pt = new PortTypeImpl();
                pt.setUndefined(false);
                pt.addOperation(wsdlOp);

                QName pt_QN = new QName("PortTypeName");

                pt.setQName(pt_QN);
                def.addPortType(pt);

                //----------------------------------

                Service s = new ServiceImpl();
                QName qn0 = new QName(serviceName);
                // javax.xml.namespace.QName qn=new QNameUtils().newQName( );
                s.setQName(qn0);

                System.out.println(" -------------------------------- ");
                //Binding b = new BindingImpl();
                Binding b = def.createBinding();
                //def.a
                b.setUndefined(false);
                QName binding_QN = new QName("SOAPBinding");
                b.setQName(binding_QN);
                b.setPortType(pt);
                //---------------------- BINDINGS -----------------------------
                BindingOperation bindOp = def.createBindingOperation();
                bindOp.setName(opName);
                SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension(BindingOperation.class, new QName(soap, "operation"));
                soapOperation.setStyle("document");
                //NOTA-BENE: Come settare SOAPACTION? jolie usa SOAP1.1 o 1.2? COme usa la SoapAction?
                soapOperation.setSoapActionURI(opName);
                bindOp.addExtensibilityElement(soapOperation);
                bindOp.setOperation(wsdlOp);
                BindingInput bindingInput = def.createBindingInput();
                SOAPBody inputBody = (SOAPBody) extensionRegistry.createExtension(BindingInput.class, new QName(soap, "body"));
                inputBody.setUse("literal");
                bindingInput.addExtensibilityElement(inputBody);
                bindOp.setBindingInput(bindingInput);
                BindingOutput bindingOutput = def.createBindingOutput();
                bindingOutput.addExtensibilityElement(inputBody);
                bindOp.setBindingOutput(bindingOutput);
                /*TODO Aggiungere il Fault
                BindingFault  bindingFault = def.createBindingFault();
                //SOAPFault soapFault = (SOAPFault) extensionRegistry.createExtension(Fault.class, new QName(soap, "fault"));
                bindingFault.addExtensibilityElement(inputBody);
                bindOp.addBindingFault(bindingFault);
                 */
                PortType portType = def.createPortType();
                portType.setQName(new QName("", serviceName + "PortType"));
                portType.addOperation(wsdlOp);
                Binding bind = def.createBinding();
                bind.setQName(new QName("", serviceName + "Binding"));
                bind.setPortType(portType);
                bind.setUndefined(false);
                SOAPBinding soapBinding = (SOAPBinding) extensionRegistry.createExtension(Binding.class, new QName(soap, "binding"));
                soapBinding.setTransportURI(soapOverHttp);
                soapBinding.setStyle("document");
                bind.addExtensibilityElement(soapBinding);
                bind.addBindingOperation(bindOp);


                Port port = def.createPort();
                port.setName(serviceName + "Port");
                SOAPAddress soapAddress = (SOAPAddress) extensionRegistry.createExtension(Port.class, new QName(soap, "address"));
                soapAddress.setLocationURI("http://127.0.0.1:8080/foo");
                port.addExtensibilityElement(soapAddress);
                port.setBinding(bind);
                def.addBinding(bind);

//========================= SERVICE ===============================
                //Port p = def.createPort();
//                port.setName("SOAPPort");
//                port.setBinding(b);
                s.addPort(port);

                def.addService(s);
                ww.writeWSDL(def, fw);

                //-----------------------------------
                System.out.println(" -------------------------------- ");
                rr = r.readWSDL(fileName);
                //rr.
                System.out.println(rr.toString());
            } catch (IOException ex) {
                Logger.getLogger(WsdlEffectorImplWsdl4J.class.getName()).log(Level.SEVERE, null, ex);
            } catch (WSDLException ex) {
                Logger.getLogger(WsdlEffectorImplWsdl4J.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    Logger.getLogger(WsdlEffectorImplWsdl4J.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

//    public static void validate(String fileName) {
//        {
//            try {
//                WSDLFactory f = WSDLFactory.newInstance();
//                WSDLReader r = f.newWSDLReader();
//                Definition def = r.readWSDL(fileName);
//                System.out.println("Validation done");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    public static void validate02(String schName, String document) {
//        javax.xml.validation.Schema schema = null;
//        try {
//            String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
//            SchemaFactory factory = SchemaFactory.newInstance(language);
//            schema = (javax.xml.validation.Schema) factory.newSchema(new File(schName));
//            Validator validator = schema.newValidator();
//            DOMSource doc = new DOMSource(document);
//            validator.validate(doc);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] arg) {
        //test00("./src/test/resources/provaInputPortsTrial00.wsdl");
        String inputArg = "./provaInputPortsTrial00.wsdl";
        test00(inputArg);
        //validate(inputArg);
    }
}
