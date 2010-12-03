/*
 * This class generates a fixed wsdl1.1 document using WSDL4J API;
 * The main purpose is to test WSDL4J API.
 *
 */
package joliex.wsdl;

import java.util.List;
import joliex.wsdl.alternative.baseversion.WsdlEffectorImplWsdl4J;
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
public class Wsdl11EffectorImplWsdl4J
{
	static String tns = "http://www.italianasoftware.com/wsdl/FirstServiceByWSDL4J.wsdl";//.wsdl
	static String tns_xsd = "http://www.italianasoftware.com/wsdl/FirstServiceByWSDL4J.xsd";//.wsdl
	//static final String tns = "http://www.italianasoftware.com/";
	static final String schema = "http://www.w3.org/2001/XMLSchema";
	static final String soap = "http://schemas.xmlsoap.org/wsdl/soap/";
	static final String wsdl = "http://schemas.xmlsoap.org/wsdl/";
	static final String soapOverHttp = "http://schemas.xmlsoap.org/wsdl/soap/http";
	static final String tns_prefix = "tns";
	static final String schema_prefix = "xs";
	static final String soap_prefix = "soap";
	static final String wsdl_prefix = "wsdl";
	static String soapOverHttp_prefix = "http://schemas.xmlsoap.org/wsdl/soap/http";
	static ExtensionRegistry extensionRegistry;
	private static WSDLFactory wsdlFactory;
	private static Definition def;

	public static Definition init( String serviceName )
	{
		try {
			wsdlFactory = WSDLFactory.newInstance();
			def = wsdlFactory.newDefinition();
			extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
			if ( serviceName != null || serviceName.isEmpty() ) {
				QName servDefQN = new QName( serviceName );
				def.setQName( servDefQN );
			}
			def.addNamespace( NameSpacesEnum.WSDL.getNameSpacePrefix(), NameSpacesEnum.WSDL.getNameSpaceURI() );
			def.addNamespace( NameSpacesEnum.SOAP.getNameSpacePrefix(), NameSpacesEnum.SOAP.getNameSpaceURI() );
			def.addNamespace( NameSpacesEnum.TNS.getNameSpacePrefix(), NameSpacesEnum.TNS.getNameSpaceURI() );
			def.addNamespace( NameSpacesEnum.XML_SCH.getNameSpacePrefix(), NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			def.setTargetNamespace( NameSpacesEnum.TNS.getNameSpaceURI() );
		} catch( WSDLException ex ) {
			Logger.getLogger( WsdlEffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return def;
	}

	public static Definition init( Definition def, String serviceName )
	{
		try {
			wsdlFactory = WSDLFactory.newInstance();
			def = wsdlFactory.newDefinition();
			extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
			if ( serviceName != null ) {
				QName servDefQN = new QName( serviceName );
				def.setQName( servDefQN );
			}
			def.addNamespace( NameSpacesEnum.WSDL.getNameSpacePrefix(), NameSpacesEnum.WSDL.getNameSpaceURI() );
			def.addNamespace( NameSpacesEnum.SOAP.getNameSpacePrefix(), NameSpacesEnum.SOAP.getNameSpaceURI() );
			def.addNamespace( NameSpacesEnum.TNS.getNameSpacePrefix(), NameSpacesEnum.TNS.getNameSpaceURI() );
			def.addNamespace( NameSpacesEnum.XML_SCH.getNameSpacePrefix(), NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			def.setTargetNamespace( NameSpacesEnum.TNS.getNameSpaceURI() );
		} catch( WSDLException ex ) {
			Logger.getLogger( WsdlEffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return def;
	}

	public static Definition setSchemaDocIntoWSDLTypes( Definition def, Element rootElement,ExtensionRegistry extensionRegistry )
	{
		try {
			//Types types=new TypesImpl();
			Types types = def.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( NameSpacesEnum.XML_SCH.getNameSpaceURI(), "schema" ) );

			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			//System.out.println( " types=" + types );
			def.setTypes( types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
		return def;
	}

//TODO rivedere meglio
	public static Document addSimpleTypeToDocument( Document document, String typeName, String minOccurs, String maxOccurs, String type )
	{		
			System.out.println( " document=" + document );
			Element elementReqSub = document.createElement( "xs:element" );
			elementReqSub.setAttribute( "minOccurs", minOccurs );
			elementReqSub.setAttribute( "maxOccurs", maxOccurs );
			elementReqSub.setAttribute( "name", typeName );
			elementReqSub.setAttribute( "type", type );
			System.out.println( " document=" + document );
		return document;
	}

//	public Definition addTypeToDocument( Definition def,Document document, String typeName, String minOccurs, String maxOccurs, String type )
//	{
//		//if (document==null) {create}
//		//def.getTypes().
//		Element elementReqSub = document.createElement( "xs:element" );
//		elementReqSub.setAttribute( "minOccurs", minOccurs );
//		elementReqSub.setAttribute( "maxOccurs", maxOccurs );
//		elementReqSub.setAttribute( "name", typeName );
//		elementReqSub.setAttribute( "type", type );
//		return def;
//	}
	public static Element addType( Element inElement, Document document, String minOccurs, String maxOccurs, String subTypeName, String type )
	{
		Element elementReqSub = document.createElement( "xs:element" );
		elementReqSub.setAttribute( "minOccurs", minOccurs );
		elementReqSub.setAttribute( "maxOccurs", maxOccurs );
		elementReqSub.setAttribute( "name", subTypeName );
		elementReqSub.setAttribute( "type", type );
		return (Element) inElement.appendChild( elementReqSub );
	}
//TODO rivedere meglio

	public static Element addComplexType( Element inElement, String elementName, Document document, String subTypeName )
	{
		Element element = document.createElement( "xs:element" );
		element.setAttribute( "name", elementName );
		Element cType = document.createElement( "xs:complexType" );
		Element sequenceFault = document.createElement( "xs:sequence" );
		Element elementFaultSub = document.createElement( "xs:element" );
		//-------------------
		elementFaultSub.setAttribute( "name", subTypeName );
		elementFaultSub.setAttribute( "type", "string" );
		sequenceFault.appendChild( elementFaultSub );
		cType.appendChild( sequenceFault );
		element.appendChild( cType );
		System.out.println( " document=" + document );
		//elementReq.appendChild(cTypeReq).appendChild(sequenceReq).appendChild(elementResp).appendChild(element);
		return (Element) inElement.appendChild( element );
	}

	public static void addTypes( String opName, String subTypeNameReq, String subTypeNameResp, String subTypeNameFault )
	{
		try {
			//Types types=new TypesImpl();
			Types types = def.createTypes();
			//TODO Come aggiungere schema (o riferimento a schema)  per i tipi?
			//sch = new SchemaImpl();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( schema, "schema" ) );
			//typesExt.
			/*TODO DeCommentare la seguente parte di codice e vedere perchè non genera l'elemento wsdl:types*/ //TODO Da qui in poi devo solo riempire lo schema con i giusti elementi...;
			//TODO Come creare elementi da settare nelle istanze della classe SchemaImpl
			Document document;
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
				 */ DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				System.out.println( " dbf=" + dbf );
				//dbf.setFeature(opName, true)
				dbf.setNamespaceAware( true );
				//dbf.setValidating(true);
				db = dbf.newDocumentBuilder();
				System.out.println( " db=" + db );
				//rootElement.setAttribute("caption", caption);
			} catch( Exception e ) {
				e.printStackTrace();
			}
			document = db.newDocument();
			System.out.println( " document=" + document );
			//DOMImplementation di=db.getDOMImplementation();
			//DocumentType dt=DocumentType();
			//document02=di.createDocument(fileName, serviceName, dt);
			//TODO veridicare le 3 linee seguenti (e le ulteriori linee)
			rootElement = document.createElement( "xs:schema" );
			rootElement.setAttribute( "xmlns:xs", schema );
			//rootElement.setAttribute("xmlns:tns",tns);
			rootElement.setAttribute( "targetNamespace", tns );
			//rootElement.setAttribute("targetNamespace:tns",tns);
			//rootElement.setAttributeNS(schema, "xmlns", schema);
			//rootElement.setAttributeNS(tns, "targetNamespace", tns);
			//rootElement.setPrefix("xs");
			//rootElement = document.createElementNS(schema,"schema");
			//TODO Usare (se esiste) un modo migliore per settare il NAMESPACE XMLSchema nei files schema e assicurarsi un corrispondente settaggio nel wsdl
			//TODO Usare un modo migliore per settare il targetNameSpace (tns) negli schema e assicurarsi un corrispondente settaggio IN TUTTO il wsdl,
			//NOTA-BENE Esempio cablato sul file di test passato in input
			//------------- inizio creazine schema --------------
			Element elementReq = document.createElement( "xs:element" );
			//elementReq.setNodeValue("nodeValue");
			elementReq.setAttribute( "name", opName );
			Element cTypeReq = document.createElement( "xs:complexType" );
			//cTypeReq..setAttibute( );
			Element sequenceReq = document.createElement( "xs:sequence" );
			Element elementReqSub = document.createElement( "xs:element" );
			elementReqSub.setAttribute( "minOccurs", "1" );
			elementReqSub.setAttribute( "maxOccurs", "1" );
			elementReqSub.setAttribute( "name", subTypeNameReq );
			elementReqSub.setAttribute( "type", "int" );
			sequenceReq.appendChild( elementReqSub );
			cTypeReq.appendChild( sequenceReq );
			//sequenceReq.setAttibute( );
			elementReq.appendChild( cTypeReq );
			Element elementResp = document.createElement( "xs:element" );
			elementResp.setAttribute( "name", opName + "Response" );
			Element cTypeResp = document.createElement( "xs:complexType" );
			Element sequenceResp = document.createElement( "xs:sequence" );
			Element elementRespSub = document.createElement( "xs:element" );
			elementRespSub.setAttribute( "minOccurs", "1" );
			elementRespSub.setAttribute( "maxOccurs", "1" );
			elementRespSub.setAttribute( "name", subTypeNameResp );
			elementRespSub.setAttribute( "type", "int" );
			sequenceResp.appendChild( elementRespSub );
			cTypeResp.appendChild( sequenceResp );
			elementResp.appendChild( cTypeResp );
			//sch.setElementType(new QName(schema, "xs"));
			//sch.setElement(elementReq);
			//http://www.w3.org/2001/03/14-annotated-WSDL-examples.html
			Element elementFault = document.createElement( "xs:element" );
			elementFault.setAttribute( "name", opName + "Fault" );
			Element cTypeFault = document.createElement( "xs:complexType" );
			Element sequenceFault = document.createElement( "xs:sequence" );
			Element elementFaultSub = document.createElement( "xs:element" );
			elementFaultSub.setAttribute( "name", subTypeNameFault );
			elementFaultSub.setAttribute( "type", "string" );
			sequenceFault.appendChild( elementFaultSub );
			cTypeFault.appendChild( sequenceFault );
			elementFault.appendChild( cTypeFault );
			System.out.println( " document=" + document );
			//elementReq.appendChild(cTypeReq).appendChild(sequenceReq).appendChild(elementResp).appendChild(element);
			rootElement.appendChild( elementReq ).appendChild( elementResp ).appendChild( elementFault );
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
			//-------------- embedding in wsdl -------------------------
			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			System.out.println( " types=" + types );
			def.setTypes( types );
		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	public static Definition initTypes( Definition def )
	{
		try {
			//Types types=new TypesImpl();
			Types types = def.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( schema, "schema" ) );
			/*TODO DeCommentare la seguente parte di codice e vedere perchè non genera l'elemento wsdl:types*/ //TODO Da qui in poi devo solo riempire lo schema con i giusti elementi...;
			//TODO Come creare elementi da settare nelle istanze della classe SchemaImpl
			Document document;
			//Document document02 = null;
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
				 */ DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				System.out.println( " dbf=" + dbf );
				//dbf.setFeature(opName, true)
				dbf.setNamespaceAware( true );
				//dbf.setValidating(true);
				db = dbf.newDocumentBuilder();
				System.out.println( " db=" + db );
				//rootElement.setAttribute("caption", caption);
			} catch( Exception e ) {
				e.printStackTrace();
			}
			document = db.newDocument();
			System.out.println( " document=" + document );
			//DOMImplementation di=db.getDOMImplementation();
			//DocumentType dt=DocumentType();
			//document02=di.createDocument(fileName, serviceName, dt);
			//TODO veridicare le 3 linee seguenti (e le ulteriori linee)
			rootElement = document.createElement( "xs:schema" );
			rootElement.setAttribute( "xmlns:xs", NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			rootElement.setAttribute( "targetNamespace", NameSpacesEnum.TNS.getNameSpaceURI() );
			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			System.out.println( " types=" + types );
			def.setTypes( types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
		return def;
	}

	public static Definition initTypeswithXSDimport( Definition def )
	{
		/*
		<definitions xmlns:tns="myWebserviceTNS" targetNamespace="myWebserviceTNS" xmlns:ts="testschema">

		<types>
		<xsd:schema elementFormDefault="qualified" targetNamespace="myWebserviceTNS">
		<xsd:import namespace="testschema" schemaLocation="./testschema.xsd"/>
		</xsd:schema>
		</types>
		</defintions>
		 */
		try {
			//Types types=new TypesImpl();
			Types types = def.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( schema, "schema" ) );
			/*TODO DeCommentare la seguente parte di codice e vedere perchè non genera l'elemento wsdl:types*/ //TODO Da qui in poi devo solo riempire lo schema con i giusti elementi...;
			//TODO Come creare elementi da settare nelle istanze della classe SchemaImpl
			Document document;
			//Document document02 = null;
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
				 */ DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				System.out.println( " dbf=" + dbf );
				//dbf.setFeature(opName, true)
				dbf.setNamespaceAware( true );
				//dbf.setValidating(true);
				db = dbf.newDocumentBuilder();
				System.out.println( " db=" + db );
				//rootElement.setAttribute("caption", caption);
			} catch( Exception e ) {
				e.printStackTrace();
			}
			document = db.newDocument();
			System.out.println( " document=" + document );
			//DOMImplementation di=db.getDOMImplementation();
			//DocumentType dt=DocumentType();
			//document02=di.createDocument(fileName, serviceName, dt);
			//TODO veridicare le 3 linee seguenti (e le ulteriori linee)
			rootElement = document.createElement( "xs:schema" );
			rootElement.setAttribute( "xmlns:xs", NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			rootElement.setAttribute( "targetNamespace", NameSpacesEnum.TNS.getNameSpaceURI() );
			//NOTW-BENE Inport xsd Namespace
			Element importElement = document.createElement( "xs:import" );
			importElement.setAttribute( "namespace", tns );
			importElement.setAttribute( "schemaLocation", "./fileName.xsd" );
			rootElement.appendChild( importElement );

			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			System.out.println( " types=" + types );
			def.setTypes( types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
		return def;
	}

	public static Definition addTypes( Definition def, String opName, String subTypeNameReq, String subTypeNameResp, String subTypeNameFault )
	{
		try {
			//Types types=new TypesImpl();
			Types types = def.createTypes();
			//TODO Come aggiungere schema (o riferimento a schema)  per i tipi?
			//sch = new SchemaImpl();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( schema, "schema" ) );
			//typesExt.
			/*TODO DeCommentare la seguente parte di codice e vedere perchè non genera l'elemento wsdl:types*/ //TODO Da qui in poi devo solo riempire lo schema con i giusti elementi...;
			//TODO Come creare elementi da settare nelle istanze della classe SchemaImpl
			Document document;
			Document document02 = null;
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
				 */ DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				System.out.println( " dbf=" + dbf );
				//dbf.setFeature(opName, true)
				dbf.setNamespaceAware( true );
				//dbf.setValidating(true);
				db = dbf.newDocumentBuilder();
				System.out.println( " db=" + db );
				//rootElement.setAttribute("caption", caption);
			} catch( Exception e ) {
				e.printStackTrace();
			}
			document = db.newDocument();
			System.out.println( " document=" + document );
			//DOMImplementation di=db.getDOMImplementation();
			//DocumentType dt=DocumentType();
			//document02=di.createDocument(fileName, serviceName, dt);
			//TODO veridicare le 3 linee seguenti (e le ulteriori linee)
			rootElement = document.createElement( "xs:schema" );
			rootElement.setAttribute( "xmlns:xs", schema );
			//rootElement.setAttribute("xmlns:tns",tns);
			rootElement.setAttribute( "targetNamespace", tns );
			//rootElement. //rootElement.setAttribute("targetNamespace:tns",tns);
			//rootElement.setAttributeNS(schema, "xmlns", schema);
			//rootElement.setAttributeNS(tns, "targetNamespace", tns);
			//rootElement.setPrefix("xs");
			//rootElement = document.createElementNS(schema,"schema");
			//TODO Usare (se esiste) un modo migliore per settare il NAMESPACE XMLSchema nei files schema e assicurarsi un corrispondente settaggio nel wsdl
			//TODO Usare un modo migliore per settare il targetNameSpace (tns) negli schema e assicurarsi un corrispondente settaggio IN TUTTO il wsdl,
			//NOTA-BENE Esempio cablato sul file di test passato in input
			//------------- inizio creazine schema --------------
			Element elementReq = document.createElement( "xs:element" );
			//elementReq.setNodeValue("nodeValue");
			elementReq.setAttribute( "name", opName );
			Element cTypeReq = document.createElement( "xs:complexType" );
			//cTypeReq..setAttibute( );
			Element sequenceReq = document.createElement( "xs:sequence" );
			Element elementReqSub = document.createElement( "xs:element" );
			elementReqSub.setAttribute( "minOccurs", "1" );
			elementReqSub.setAttribute( "maxOccurs", "1" );
			elementReqSub.setAttribute( "name", subTypeNameReq );
			elementReqSub.setAttribute( "type", "int" );
			sequenceReq.appendChild( elementReqSub );
			cTypeReq.appendChild( sequenceReq );
			//sequenceReq.setAttibute( );
			elementReq.appendChild( cTypeReq );
			Element elementResp = document.createElement( "xs:element" );
			elementResp.setAttribute( "name", opName + "Response" );
			Element cTypeResp = document.createElement( "xs:complexType" );
			Element sequenceResp = document.createElement( "xs:sequence" );
			Element elementRespSub = document.createElement( "xs:element" );
			elementRespSub.setAttribute( "minOccurs", "1" );
			elementRespSub.setAttribute( "maxOccurs", "1" );
			elementRespSub.setAttribute( "name", subTypeNameResp );
			elementRespSub.setAttribute( "type", "int" );
			sequenceResp.appendChild( elementRespSub );
			cTypeResp.appendChild( sequenceResp );
			elementResp.appendChild( cTypeResp );
			//sch.setElementType(new QName(schema, "xs"));
			//sch.setElement(elementReq);
			//http://www.w3.org/2001/03/14-annotated-WSDL-examples.html
			Element elementFault = document.createElement( "xs:element" );
			elementFault.setAttribute( "name", opName + "Fault" );
			Element cTypeFault = document.createElement( "xs:complexType" );
			Element sequenceFault = document.createElement( "xs:sequence" );
			Element elementFaultSub = document.createElement( "xs:element" );
			elementFaultSub.setAttribute( "name", subTypeNameFault );
			elementFaultSub.setAttribute( "type", "string" );
			sequenceFault.appendChild( elementFaultSub );
			cTypeFault.appendChild( sequenceFault );
			elementFault.appendChild( cTypeFault );
			System.out.println( " document=" + document );
			//elementReq.appendChild(cTypeReq).appendChild(sequenceReq).appendChild(elementResp).appendChild(element);
			rootElement.appendChild( elementReq ).appendChild( elementResp ).appendChild( elementFault );
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
			//-------------- embedding in wsdl -------------------------
			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			System.out.println( " types=" + types );
			def.setTypes( types );
		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return def;
	}

	public static Definition addRequestMessage( Definition def, String elementName, String opName )
	{
		String inputPartName = "parameters";
		Input input = def.createInput();
		Message inputMessage = def.createMessage();
		inputMessage.setUndefined( false );
		inputMessage.setQName( new QName( opName + "Request" ) );
		Part inputPart = def.createPart();
		inputPart.setName( inputPartName );
		//inputPart.setTypeName( new QName( "http://www.w3.org/2001/XMLSchema", inputPartType ) );
		//inputPart.setElementName( new QName( tns, inputPartType ) );
		inputPart.setElementName( new QName( elementName ) );
		inputMessage.addPart( inputPart );
		input.setMessage( inputMessage );
		def.addMessage( inputMessage );
		return def;
	}

	public static Message addRequestMessage( String msgTypeName, String opName )
	{
		String inputPartName = "parameters";
		Input input = def.createInput();
		Message inputMessage = def.createMessage();
		inputMessage.setUndefined( false );
		inputMessage.setQName( new QName( msgTypeName ) );
		Part inputPart = def.createPart();
		inputPart.setName( inputPartName );
		//inputPart.setTypeName( new QName( "http://www.w3.org/2001/XMLSchema", inputPartType ) );
		//inputPart.setElementName( new QName( tns, inputPartType ) );
		inputPart.setElementName( new QName( opName + "Request" ) );
		inputMessage.addPart( inputPart );
		input.setMessage( inputMessage );
		def.addMessage( inputMessage );
		return inputMessage;
	}

	

	public static Definition addResponseMessage( Definition def,  String opName, String msgTypeName )
	{
		String outputPartName = "parameters";
//		Output output = def.createOutput();
//		Message outputMessage = def.createMessage();
//		output.setMessage( outputMessage );

		Message msg_resp = new MessageImpl();
		msg_resp.setUndefined( false );

		msg_resp.setQName( new QName( msgTypeName ) );
		Part p02a = new PartImpl();
		p02a.setName( outputPartName );//style=document
		p02a.setElementName( new QName( opName + "Response" ) );
		msg_resp.addPart( p02a );
		def.addMessage( msg_resp );
		return def;
	}

	public static Message addResponseMessage(  String msgName,String opName )
	{
		String outputPartName = "parameters";
//		Output output = def.createOutput();
//		Message outputMessage = def.createMessage();
//		output.setMessage( outputMessage );

		Message msg_resp = new MessageImpl();
		msg_resp.setUndefined( false );

		msg_resp.setQName( new QName( msgName ) );
		Part p02a = new PartImpl();
		p02a.setName( outputPartName );//style=document
		p02a.setElementName( new QName( opName + "Response" ) );
		msg_resp.addPart( p02a );
		def.addMessage( msg_resp );
		return msg_resp;
	}

	public static Operation addPortType( Definition def,String portTypeName, OperationType ot, String opName, String msg_req_str, String msg_resp_str )
	{
		//NOTA-BENE i 4 tipi di primitive dicomm sono distitne dalla presenzao meno di elementi input output in questa sezione dentro le operazioni
		Operation wsdlOp = def.createOperation();

		wsdlOp.setName( opName );
		wsdlOp.setStyle( ot );
		wsdlOp.setUndefined( false );

		Input in = new InputImpl();
		in.setName( "inputInName" );
		Message msg_req = def.getMessage( new QName( msg_req_str ) );
		in.setMessage( msg_req );
		wsdlOp.setInput( in );

		Output out = new OutputImpl();
		out.setName( "outputOutName" );
		Message msg_resp = def.getMessage( new QName( msg_resp_str ) );
		out.setMessage( msg_resp );
		wsdlOp.setOutput( out );

		Fault fault = new FaultImpl();
		fault.setName( "faultName" );
		fault.setMessage( msg_resp );
		wsdlOp.addFault( fault );

//-----------------------------------------------------
		PortType pt = new PortTypeImpl();
		pt.setUndefined( false );
		pt.addOperation( wsdlOp );

		QName pt_QN = new QName( portTypeName );

		pt.setQName( pt_QN );
		def.addPortType( pt );
		return wsdlOp;
	}

	public static Definition initPortType( Definition def, String portTypeName, OperationType ot, String opName, String msg_req_str, String msg_resp_str )
	{
		//NOTA-BENE i 4 tipi di primitive dicomm sono distitne dalla presenzao meno di elementi input output in questa sezione dentro le operazioni
//		Operation wsdlOp = def.createOperation();
//
//		wsdlOp.setName( opName );
//		wsdlOp.setStyle( ot );
//		wsdlOp.setUndefined( false );

//		Input in = new InputImpl();
//		in.setName( "inputInName" );
//		Message msg_req = def.getMessage( new QName( msg_req_str ) );
//		in.setMessage( msg_req );
//		wsdlOp.setInput( in );

//		Output out = new OutputImpl();
//		out.setName( "outputOutName" );
//		Message msg_resp = def.getMessage( new QName( msg_resp_str ) );
//		out.setMessage( msg_resp );
//		wsdlOp.setOutput( out );

//		Fault fault = new FaultImpl();
//		fault.setName( "faultName" );
//		fault.setMessage( msg_resp );
//		wsdlOp.addFault( fault );

//-----------------------------------------------------
		PortType pt = new PortTypeImpl();
		pt.setUndefined( false );
//		pt.addOperation( wsdlOp );

		QName pt_QN = new QName( portTypeName );

		pt.setQName( pt_QN );
		def.addPortType( pt );
		return def;
	}

	public static ExtensionRegistry addBindingSOAP(Definition def,ExtensionRegistry extensionRegistry, String pt_name, Operation wsdlOp/* togliere questo!*/, String opName, String serviceName )
	{
		try {
			PortType portType = def.getPortType( new QName( pt_name ) );
			//Operation op= def.getBinding( null ).getBindingOperation( tns, tns, tns ).getOperation()
			BindingOperation bindOp = def.createBindingOperation();
			bindOp.setName( opName );
			SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension( BindingOperation.class, new QName( soap, "operation" ) );
			soapOperation.setStyle( "document" );
			//NOTA-BENE: Come settare SOAPACTION? jolie usa SOAP1.1 o 1.2? COme usa la SoapAction?
			soapOperation.setSoapActionURI( opName );
			bindOp.addExtensibilityElement( soapOperation );
			//Operation wsdlOp00=def.getPortType(new QName(pt_name)).getOperation( opName, "", "" );
			//System.out.println(" wsdlOp00="+wsdlOp00);
			Operation wsdlOp0=def.createOperation();
			//wsdlOp.setName( opName );
			//bindOp.setOperation( wsdlOp );
			bindOp.setOperation( wsdlOp );
			BindingInput bindingInput = def.createBindingInput();
			SOAPBody inputBody = (SOAPBody) extensionRegistry.createExtension( BindingInput.class, new QName( soap, "body" ) );
			inputBody.setUse( "literal" );
			bindingInput.addExtensibilityElement( inputBody );
			bindOp.setBindingInput( bindingInput );
			BindingOutput bindingOutput = def.createBindingOutput();
			bindingOutput.addExtensibilityElement( inputBody );
			bindOp.setBindingOutput( bindingOutput );
			/*TODO Aggiungere il Fault
			BindingFault  bindingFault = def.createBindingFault();
			//SOAPFault soapFault = (SOAPFault) extensionRegistry.createExtension(Fault.class, new QName(soap, "fault"));
			bindingFault.addExtensibilityElement(inputBody);
			bindOp.addBindingFault(bindingFault);
			 */
//				PortType portType = def.createPortType();
//				portType.setQName( new QName( "", serviceName + "PortType" ) );
//				portType.addOperation( wsdlOp );
			Binding bind = def.createBinding();
			//def.getServices();
			//bind.setQName( new QName( tns, serviceName + "Binding" ) );
			bind.setQName( new QName( serviceName + "Binding" ) );
			bind.setPortType( portType );
			bind.setUndefined( false );
			SOAPBinding soapBinding = (SOAPBinding) extensionRegistry.createExtension( Binding.class, new QName( soap, "binding" ) );
			soapBinding.setTransportURI( soapOverHttp );
			soapBinding.setStyle( "document" );
			bind.addExtensibilityElement( soapBinding );
			bind.addBindingOperation( bindOp );
			def.addBinding( bind );

		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return extensionRegistry;
	}

	public static void addService( String serviceName, String bindingName, String mySOAPAddress )
	{//String mySOAPAddress = "http://127.0.0.1:8080/foo";
		try {
			Binding bind = def.getBinding( new QName( bindingName ) );
			Port p = def.createPort();
			p.setName( serviceName + "Port" );
			SOAPAddress soapAddress = (SOAPAddress) extensionRegistry.createExtension( Port.class, new QName( soap, "address" ) );
			soapAddress.setLocationURI( mySOAPAddress );
			p.addExtensibilityElement( soapAddress );
			p.setBinding( bind );
			Service s = new ServiceImpl();
			QName qn0 = new QName( serviceName );
			// javax.xml.namespace.QName qn=new QNameUtils().newQName( );
			s.setQName( qn0 );
			s.addPort( p );
			def.addService( s );
		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	public static WSDLWriter getWSDLWriter( Definition def )
	{
		WSDLWriter ww = wsdlFactory.newWSDLWriter();
		return ww;
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
	public static void main( String[] arg )
	{
		FileWriter fw = null;
		try {
			//test00("./src/test/resources/provaInputPortsTrial00.wsdl");
			String inputArg00 = "./provaInputPortsTrial00.wsdl";
			String inputArg01 = "./provaInputPortsTrial01.wsdl";
			//test00( inputArg00 );
			String serviceName = "TwiceService";
			String opName = "twice";
			String subTypeNameReq = "num";
			String subTypeNameResp = "result";
			String subTypeNameFault = "fault";
			Definition def01 = init( serviceName );
			addTypes( opName, subTypeNameReq, subTypeNameResp, subTypeNameFault );
			Message msg_req = addRequestMessage( opName, serviceName );

			Message msg_resp = addResponseMessage( opName, serviceName );

			Operation wsdlOp = addPortType( def,"PortTypeName", OperationType.REQUEST_RESPONSE, opName, msg_req.getQName().toString(), msg_resp.getQName().toString() );

			addBindingSOAP( def,extensionRegistry,"PortTypeName", wsdlOp, opName, serviceName );

			addService( serviceName, serviceName + "Binding", "MySOAPAddress" );
			/*
			 * *
			 */ //validate(inputArg00);
			WSDLFactory f = WSDLFactory.newInstance();
			WSDLReader r = f.newWSDLReader();
			//r.setExtensionRegistry(null);
			r.setFeature( "javax.wsdl.verbose", true );
			r.setFeature( "javax.wsdl.importDocuments", true );
			//r.readWSDL("");
			WSDLWriter ww = f.newWSDLWriter();
			fw = new FileWriter( inputArg01 );
			ww.writeWSDL( def, fw );
		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		} catch( IOException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		} finally {
			try {
				fw.close();
			} catch( IOException ex ) {
				Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
			}
		}
	}
}
