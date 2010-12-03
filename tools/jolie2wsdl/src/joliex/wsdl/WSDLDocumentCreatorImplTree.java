/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.wsdl;

import com.ibm.wsdl.FaultImpl;
import com.ibm.wsdl.InputImpl;
import com.ibm.wsdl.MessageImpl;
import com.ibm.wsdl.OutputImpl;
import com.ibm.wsdl.PartImpl;
import com.ibm.wsdl.PortTypeImpl;
import com.ibm.wsdl.ServiceImpl;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Binding;
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
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.runtime.RequestResponseOperation;
import joliex.wsdl.validation.Wsdl11EffectorImplWsdl4Jtypes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import joliex.wsdl.support.GeneralDocumentCreator;
import joliex.wsdl.support.GeneralProgramVisitor;
import joliex.wsdl.support.treeOLObject;

/**
 *
 * @author Francesco
 */
public class WSDLDocumentCreatorImplTree extends GeneralDocumentCreator
{
	//private File outputDirectory;
	private Set<RequestResponseOperationDeclaration> requestResponce;
	private Set<OneWayOperationDeclaration> oneWay;
	private String nameFile = "output";//TODO Far Arrivare fin qua il nome del file
	private List<String> nameFiles;
	private Set<TypeDefinition> types;
	private File outputDirectory;
	//WATCH-OUT Direct usage of wsdl4j lib;
	//TODO refactor to parametrize on implementations lib
	//TODO refactor to parametrize on tecnological switches (wsdl1,soap1.1|soap1.2) (wsdl2,...)
	//see also: spring-ws providers libs
	static ExtensionRegistry extensionRegistry;
	private static WSDLFactory wsdlFactory;
	private Definition localDef = null;
	private WSDLWriter ww = null;
	private Writer fw = null;

	public WSDLDocumentCreatorImplTree( GeneralProgramVisitor genProgramVisitor )
	{
		super( genProgramVisitor );

//		outputDirectory = new File( "" );
//		requestResponce = new HashSet<RequestResponseOperationDeclaration>();
//		oneWay = new HashSet<OneWayOperationDeclaration>();
//		types = new LinkedHashSet<TypeDefinition>();

	}

	public Definition initWsdl( String serviceName )
	{
		try {
			wsdlFactory = WSDLFactory.newInstance();
			localDef = wsdlFactory.newDefinition();
			extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
			if ( serviceName != null ) {
				QName servDefQN = new QName( serviceName );
				localDef.setQName( servDefQN );
			}
			localDef.addNamespace( NameSpacesEnum.WSDL.getNameSpacePrefix(), NameSpacesEnum.WSDL.getNameSpaceURI() );
			localDef.addNamespace( NameSpacesEnum.SOAP.getNameSpacePrefix(), NameSpacesEnum.SOAP.getNameSpaceURI() );
			localDef.addNamespace( NameSpacesEnum.TNS.getNameSpacePrefix(), NameSpacesEnum.TNS.getNameSpaceURI() );
			localDef.addNamespace( NameSpacesEnum.XML_SCH.getNameSpacePrefix(), NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			localDef.setTargetNamespace( NameSpacesEnum.TNS.getNameSpaceURI() );
			fw = new FileWriter( nameFile + ".wsdl" );
		} catch( IOException ex ) {
			Logger.getLogger( WSDLDocumentCreatorImplTree.class.getName() ).log( Level.SEVERE, null, ex );
		} catch( WSDLException ex ) {
			Logger.getLogger( WSDLDocumentCreatorImplTree.class.getName() ).log( Level.SEVERE, null, ex );
		}

		return localDef;
	}

	@Override
	public void ConvertDocument()
	{
		System.out.println( " ========== CONVERTDOC START ==========" );
		Element rootElement = null;
		try {
			initWsdl( "hardwiredServiceName" );
			Document doc = null;
			Document document = Wsdl11EffectorImplWsdl4Jtypes.createDocument();
			System.out.println( " document="+document );
			//Wsdl11EffectorImplWsdl4Jtypes.
			//FIXME NOTA-BENE Il Doc è null
			//Document doc = initSchemaDocument();
			//Wsdl11EffectorImplWsdl4J.initTypes( localDef );
			//Wsdl11EffectorImplWsdl4J.initTypes( localDef );

			List<treeOLObject> t_list = this.GetOlTree();

			for( treeOLObject t_o : t_list ) { //PORTE
				//System.out.println( " #InputPortSize=" + t_o.GetLinkedObjetSize() );

				if ( t_o.GetOLSyntaxNode() instanceof InputPortInfo ) {
					InputPortInfo ips = (InputPortInfo) t_o.GetOLSyntaxNode();
					URI SOAPAddres = ips.location();
					if ( !ips.protocolId().equals( "soap" ) ) {
						System.err.println( "ERROR: jolie2wsdl only support soap port declarations in jolie files" );
						//TODO decidere coem gestire
						//throw new Jolie2wsdlException();
					}
					//-------------- INPUTPORT
					for( int i = 0; i < t_o.GetLinkedObjetSize(); i++ ) {  //INTERFACCIE
						InterfaceDefinition interf = (InterfaceDefinition) t_o.GetLinkedObject( i ).GetOLSyntaxNode();
						String portTypeName = interf.name() + "PortType";
						//-------------- PORTTYPE
						for( int j = 0; j < t_o.GetLinkedObject( i ).GetLinkedObjetSize(); j++ ) {//OPERATIONS
							if ( t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetOLSyntaxNode() instanceof OneWayOperationDeclaration ) {
								//-------------- OPERATION
								OneWayOperationDeclaration owop = (OneWayOperationDeclaration) t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetOLSyntaxNode();
								TypeDefinition msgReqTypes = (TypeDefinition) t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( 0 ).GetOLSyntaxNode();
								//--------------
								//TODO Aggiungere portType con op 1way cioè SENZA msg di risposta
								Operation wsdlOp = addPortType( localDef, portTypeName, OperationType.ONE_WAY, portTypeName, msgReqTypes.id(), " " );
								//TODO NON PASSARE OPERATION COSI!!!
								addBindingSOAP( portTypeName, wsdlOp, owop.id(), owop.id() );
								addRequestMessage( owop.id(), msgReqTypes.id() );
								addService( "serviceName", "myBindingName", SOAPAddres.toString() );
								//-----------------
								if ( t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( 0 ).GetLinkedObjetSize() != 0 ) {
									for( int k = 0; k < t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObjetSize(); k++ ) {
										//in quest aversione subTypeRec internamente costruisce il document
										subTypeRec( t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( k ) );

										//TODO setSchemaDocIntoWSDLTypes(doc.getDocumentElement());
									}
								}
							} else {  // REQUEST-RESPONSE-OP
								if ( t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetOLSyntaxNode() instanceof RequestResponseOperationDeclaration ) {
									//-------------- OPERATION
									RequestResponseOperationDeclaration twop = (RequestResponseOperationDeclaration) t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetOLSyntaxNode();
									TypeDefinition msgReqTypes = (TypeDefinition) t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( 0 ).GetOLSyntaxNode();
									TypeDefinition msgRespTypes = (TypeDefinition) t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( 1 ).GetOLSyntaxNode();
									//-------------- My LOGICAL MATCHING -------
									String reqName = msgReqTypes.id();
									String respName = msgRespTypes.id();

									Operation wsdlOp = addPortType( localDef, portTypeName, OperationType.REQUEST_RESPONSE, portTypeName, reqName, respName );
									//TODO NON PASSARE OPERATION COSI!!!
									addBindingSOAP( portTypeName, wsdlOp, twop.id(), twop.id() );
									addRequestMessage( twop.id(), reqName );//NOTA-BENE opName
									addResponseMessage( twop.id(), respName );//NOTA-BENE opNameResponse
									addService( "serviceName", "myBindingName", SOAPAddres.toString() );
									//setHardwiredSchemaDocIntoWSDLTypes( opName, subTypeNameReq, subTypeNameReqType, subTypeNameResp, subTypeNameRespType );

									//doc = initSchemaDocument();
									String opName = twop.id();
									//Attenzione: SUB TYPES qui

									//rootElement = Wsdl11EffectorImplWsdl4Jtypes.appendElementToElementViaDoc( document, opName, rootElement, elementFault );
									if ( t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( 0 ).GetLinkedObjetSize() != 0 ) {
										for( int k = 0; k < t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObjetSize(); k++ ) {
											//k=1 => request (?)
											//if (k==1) {subTypeRec( document, t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( k ) );}
											//TODO fare cast a LINNKEDTYPE

											List<TypeDefinition> td_list = new ArrayList<TypeDefinition>();
											if ( (t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( k ).GetOLSyntaxNode() instanceof TypeDefinition) ) {

												for( int l = 0; l < t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( k ).GetLinkedObjetSize(); l++ ) {

													subTypeRec( document, t_o.GetLinkedObject( i ).GetLinkedObject( j ).GetLinkedObject( k ).GetLinkedObject( l ) );
												}
											}
										}
									}
								}
							}//Req REsp OP
						}//Operations
                    //TODO OUTPUTPORTS
//					if ( t_o.GetOLSyntaxNode() instanceof InputPortInfo ) {
//						//TODO Fare outputPort
//						OutputPortInfo ips = (OutputPortInfo) t_o.GetOLSyntaxNode();
//					}
					}
				}
			}
			//System.out.println( " doc.getDocumentElement()=" + doc.getDocumentElement() );

			//setSchemaDocIntoWSDLTypes( doc);//versionecon Doc passato
			setSchemaDocIntoWSDLTypes( rootElement );
//			String subTypeNameReq = "mySubTypeNameReq";
//			String subTypeNameReqType = "int";
//			String subTypeNameResp = "mySubTypeNameResp";
//			String subTypeNameRespType = "int";
//			String opName = "myOpName";
			//setHardwiredSchemaDocIntoWSDLTypes( opName, subTypeNameReq, subTypeNameReqType, subTypeNameResp, subTypeNameRespType );
			WSDLWriter ww = wsdlFactory.newWSDLWriter();
			ww.writeWSDL( localDef, fw );
		} // end ConvertDocument
		catch( WSDLException ex ) {
			System.err.println( ex.getMessage() );
			Logger.getLogger( WSDLDocumentCreatorImplTree.class.getName() ).log( Level.SEVERE, null, ex );
		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
			Logger.getLogger( WSDLDocumentCreatorImplTree.class.getName() ).log( Level.SEVERE, null, ex );
		}
		System.out.println( " ========== CONVERTDOC END ==========" );
	}// end ConvertDocument

	@Override
	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException
	{
//		System.out.println( this.getClass().getCanonicalName() + " Interface=" + interfaceDefinition );
	}

	@Override
	public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException
	{
//		System.out.println( this.getClass().getCanonicalName() + " outPort=" + outputPortInfo );
	}

	@Override
	public void ConvertInputPorts( InputPortInfo inputPortInfo, Writer writer )
		throws IOException
	{
//		if ( inputPortInfo.protocolId() == "soap" ) {
//			List<String> il = inputPortInfo.getInterfacesList();
//			//TODO
//			//Wsdl11EffectorImplWsdl4J.addPortType( inputPortInfo.id(), inputPortInfo, "opName", "", "" );
//			System.out.println( this.getClass().getCanonicalName() + " InPort=" + inputPortInfo );
//		} else {
//			//Conversione in wsdl non eseguita
//		}
	}

	@Override
	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
		throws IOException
	{
//		System.out.println( this.getClass().getCanonicalName() + " 1W-OP=" + operationDeclaration );
//		System.out.println( this.getClass().getCanonicalName() + " RR-OP=" + operationDeclaration );
	}

	@Override
	public void ConvertTypes( TypeDefinition typesDefinition, Writer writer )
		throws IOException
	{
	}

	private Document initSchemaDocument()
	{

		Document document = null;
		Element rootElement = null;
		DocumentBuilder db = null;
		//try {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		//dbf.setFeature(opName, true)
		dbf.setNamespaceAware( true );
		dbf.setValidating( true );
		try {
			db = dbf.newDocumentBuilder();
			//rootElement.setAttribute("caption", caption);
		} catch( ParserConfigurationException ex ) {
			Logger.getLogger( WSDLDocumentCreatorImplTree.class.getName() ).log( Level.SEVERE, null, ex );
		}
		//rootElement.setAttribute("caption", caption);

		document = db.newDocument();

		rootElement = document.createElement( "xs:schema" );
		rootElement.setAttribute( "xmlns:xs", NameSpacesEnum.XML_SCH.getNameSpaceURI() );
		//rootElement.setAttribute("xmlns:tns",tns);
		rootElement.setAttribute( "targetNamespace", NameSpacesEnum.TNS.getNameSpaceURI() );
		System.out.println( " initSchemaDocument:document=" + document );

		return document;
	}

	private void addRequestMessage( String opName, String msgTypeName )
	{
		String inputPartName = "parameters";
		Input input = localDef.createInput();
		Message inputMessage = localDef.createMessage();
		inputMessage.setUndefined( false );
		inputMessage.setQName( new QName( msgTypeName ) );
		Part inputPart = localDef.createPart();
		inputPart.setName( inputPartName );
		//inputPart.setTypeName( new QName( "http://www.w3.org/2001/XMLSchema", inputPartType ) );
		//inputPart.setElementName( new QName( tns, inputPartType ) );
		inputPart.setElementName( new QName( opName ) );
		inputMessage.addPart( inputPart );
		input.setMessage( inputMessage );
		localDef.addMessage( inputMessage );
	}

	private void addResponseMessage( String opName, String msgName )
	{
		String outputPartName = "parameters";
		Output output = localDef.createOutput();
		Message outputMessage = localDef.createMessage();
		outputMessage.setUndefined( false );
		output.setMessage( outputMessage );

		Message msg_resp = new MessageImpl();
		msg_resp.setUndefined( false );

		msg_resp.setQName( new QName( msgName ) );
		Part p02a = new PartImpl();
		p02a.setName( outputPartName );//style=document
		p02a.setElementName( new QName( opName + "Response" ) );
		msg_resp.addPart( p02a );
		localDef.addMessage( msg_resp );

	}

	//FAULT
	private Operation addPortType( Definition def, String portTypeName, OperationType ot, String opName, String msg_req_str, String msg_resp_str )
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

	private void addBindingSOAP( String pt_name, Operation wsdlOp/* togliere questo!*/, String opName, String serviceName )
	{
		try {
			PortType portType = localDef.getPortType( new QName( pt_name ) );
			//Operation op= def.getBinding( null ).getBindingOperation( tns, tns, tns ).getOperation()
			BindingOperation bindOp = localDef.createBindingOperation();
			bindOp.setName( opName );
			SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension( BindingOperation.class, new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "operation" ) );
			soapOperation.setStyle( "document" );
			//NOTA-BENE: Come settare SOAPACTION? jolie usa SOAP1.1 o 1.2? COme usa la SoapAction?
			soapOperation.setSoapActionURI( opName );
			bindOp.addExtensibilityElement( soapOperation );
			//Operation wsdlOp00=def.getPortType(new QName(pt_name)).getOperation( opName, "", "" );
			//System.out.println(" wsdlOp00="+wsdlOp00);
			Operation wsdlOp0 = localDef.createOperation();
			//wsdlOp.setName( opName );
			//bindOp.setOperation( wsdlOp );
			bindOp.setOperation( wsdlOp );
			BindingInput bindingInput = localDef.createBindingInput();
			SOAPBody inputBody = (SOAPBody) extensionRegistry.createExtension( BindingInput.class, new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "body" ) );
			inputBody.setUse( "literal" );
			bindingInput.addExtensibilityElement( inputBody );
			bindOp.setBindingInput( bindingInput );
			BindingOutput bindingOutput = localDef.createBindingOutput();
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
			Binding bind = localDef.createBinding();
			//def.getServices();
			//bind.setQName( new QName( tns, serviceName + "Binding" ) );
			bind.setQName( new QName( serviceName + "Binding" ) );
			bind.setPortType( portType );
			bind.setUndefined( false );
			SOAPBinding soapBinding = (SOAPBinding) extensionRegistry.createExtension( Binding.class, new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "binding" ) );
			soapBinding.setTransportURI( NameSpacesEnum.SOAPoverHTTP.getNameSpaceURI() );
			soapBinding.setStyle( "document" );
			bind.addExtensibilityElement( soapBinding );
			bind.addBindingOperation( bindOp );
			localDef.addBinding( bind );

		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}

	}

	public void addService( String serviceName, String bindingName, String mySOAPAddress )
	{//String mySOAPAddress = "http://127.0.0.1:8080/foo";
		try {
			Binding bind = localDef.getBinding( new QName( bindingName ) );
			Port p = localDef.createPort();
			p.setName( serviceName + "Port" );
			SOAPAddress soapAddress = (SOAPAddress) extensionRegistry.createExtension( Port.class, new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "address" ) );
			soapAddress.setLocationURI( mySOAPAddress );
			p.addExtensibilityElement( soapAddress );
			p.setBinding( bind );
			Service s = new ServiceImpl();
			QName qn0 = new QName( serviceName );
			s.setQName( qn0 );
			s.addPort( p );
			localDef.addService( s );
		} catch( WSDLException ex ) {
			Logger.getLogger( Wsdl11EffectorImplWsdl4J.class.getName() ).log( Level.SEVERE, null, ex );
		}
	}

	private Element subTypeRec( Document doc, treeOLObject t )
	{
		//OLD Document doc = createDOMdocument();
		System.out.println( " doc=" + doc);
		if ( t.GetLinkedObjetSize() == 0 ) {
			//isNative
//				if (t.GetOLSyntaxNode() istanceof ) {//					
//				}
			System.out.println( " SIZE=0" );
			System.out.println( " subTypeRec:LeafType=" + ((TypeDefinition) t.GetOLSyntaxNode()).id()+": "+((TypeDefinition) t.GetOLSyntaxNode()).nativeType().id() );
			//doc = addNativeTypeToDoc( doc, "nomeDelCampoDelTipo", "int" );
		} else {//RICORSIONE
			if ( t.GetOLSyntaxNode() instanceof TypeDefinitionLink ) {
				TypeDefinitionLink tdl = (TypeDefinitionLink) t.GetOLSyntaxNode();
				System.out.println( " subTypeRec:NESTEDType=" +tdl.id() );

			} else {
				if ( t.GetOLSyntaxNode() instanceof TypeInlineDefinition ) {
					TypeInlineDefinition tin = (TypeInlineDefinition) t.GetOLSyntaxNode();
					System.out.println( " subTypeRec:NESTEDType=" +tin.id() );
				} else {
					System.out.println( " type cast non previsto" );
				}
			}
			
			for( int k = 0; k < t.GetLinkedObjetSize(); k++ ) {
				System.out.println( " calling subTyprRec " );
				subTypeRec( doc,t.GetLinkedObject( k ) );
				//TODO addComplexType();
			}
		}

		return doc.getDocumentElement();
	}

	private Document subTypeRec( treeOLObject t )
	{
		Document doc = createDOMdocument();
		//System.out.println( " doc=" + doc);
		if ( t.GetLinkedObjetSize() == 0 ) {
			//isNative
			//OLSyntaxNode nt = t.GetOLSyntaxNode();
			//System.out.println( " NativeType=" + t.GetOLSyntaxNode() );
			//doc = addNativeTypeToDoc( doc, "nomeDelCampoDelTipo", "int" );
		} else {
			//isTypeLink
			//System.out.println( " Type=" + t.GetOLSyntaxNode() );
			for( int k = 0; k < t.GetLinkedObject( 0 ).GetLinkedObjetSize(); k++ ) {
				subTypeRec( t.GetLinkedObject( k ) );
				//TODO addComplexType();
			}
		}
		System.out.println( " subTypeRec:doc=" + doc );
		return doc;
	}

	private Document createDOMdocument()
	{
		Document document;
		Element rootElement = null;
		DocumentBuilder db = null;
		try {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			//dbf.setFeature(opName, true)
			dbf.setNamespaceAware( true );
			//dbf.setValidating(true);
			db = dbf.newDocumentBuilder();
			//rootElement.setAttribute("caption", caption);
		} catch( Exception e ) {
			e.printStackTrace();
		}
		document = db.newDocument();
		rootElement = document.createElement( "xs:schema" );
		rootElement.setAttribute( "xmlns:xs", NameSpacesEnum.XML_SCH.getNameSpaceURI() );
		rootElement.setAttribute( "targetNamespace", NameSpacesEnum.TNS.getNameSpaceURI() );
		document.appendChild( rootElement );
		System.out.println( " createDOMdocument:document=" + document );
		return document;
	}

	public Document addElement( Document document, String minOccurs, String maxOccurs, String elementName, String elementType )
	{
		Element elementReqSub = document.createElement( "xs:element" );
		elementReqSub.setAttribute( "minOccurs", minOccurs );
		elementReqSub.setAttribute( "maxOccurs", maxOccurs );
		elementReqSub.setAttribute( "name", elementName );
		elementReqSub.setAttribute( "type", elementType );
		//inElement.appendChild( elementReqSub );
		return document;
	}

	public Element addElementToElement( Document document, Element inElement, String minOccurs, String maxOccurs, String elementName, String elementType )
	{
		Element elementReqSub = document.createElement( "xs:element" );
		elementReqSub.setAttribute( "minOccurs", minOccurs );
		elementReqSub.setAttribute( "maxOccurs", maxOccurs );
		elementReqSub.setAttribute( "name", elementName );
		elementReqSub.setAttribute( "type", elementType );
		inElement.appendChild( elementReqSub );
		return inElement;
	}

	private Document addNativeTypeToDoc( Document document, Element inElement, String subTypeName, String type )
	{
		Element elementReqSub = document.createElement( "xs:element" );
		elementReqSub.setAttribute( "name", subTypeName );
		elementReqSub.setAttribute( "type", "xs:" + type );
		inElement.appendChild( elementReqSub );
		return document;
	}

	private Document addNativeTypeToDoc( Document document, String subTypeName, String type )
	{
		//TODO fare append al document
		Element elementReqSub = document.createElement( "xs:element" );
		elementReqSub.setAttribute( "name", subTypeName );
		elementReqSub.setAttribute( "type", "xs:" + type );
		//inElement.appendChild( elementReqSub );
		System.out.println( " document=" + document );
		//document.appendChild( document.getDocumentElement() );
		System.out.println( " document=" + document );
		return document;
	}

	private Document addType( Element inElement, Document document, String minOccurs, String maxOccurs, String subTypeName, String type )
	{
		Element elementReqSub = document.createElement( "xs:element" );
		elementReqSub.setAttribute( "minOccurs", minOccurs );
		elementReqSub.setAttribute( "maxOccurs", maxOccurs );
		elementReqSub.setAttribute( "name", subTypeName );
		elementReqSub.setAttribute( "type", type );
		inElement.appendChild( elementReqSub );
		return document;
	}
//TODO rivedere meglio

	private Element addComplexType( Element inElement, String elementName, Document document, String subTypeName )
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

	private void setSchemaDocIntoWSDLTypes( Document document )
	{
		try {
			//Types types=new TypesImpl();
			Types types = localDef.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( NameSpacesEnum.XML_SCH.getNameSpaceURI(), "schema" ) );



			typesExt.setElement( document.getDocumentElement() );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			//System.out.println( " types=" + types );
			localDef.setTypes( types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
	}

	public void setSchemaDocIntoWSDLTypes( Element rootElement )
	{
		try {
			//Types types=new TypesImpl();
			Types types = localDef.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( NameSpacesEnum.XML_SCH.getNameSpaceURI(), "schema" ) );



			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			//System.out.println( " types=" + types );
			localDef.setTypes( types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
	}

	private void setHardwiredSchemaDocIntoWSDLTypes( String opName, String subTypeNameReq, String subTypeNameReqType, String subTypeNameResp, String subTypeNameRespType )
	{
		try {
			//Types types=new TypesImpl();
			Types types = localDef.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class, new QName( NameSpacesEnum.XML_SCH.getNameSpaceURI(), "schema" ) );

			Document document;
			Element rootElement = null;
			DocumentBuilder db = null;
			try {

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				//dbf.setFeature(opName, true)
				dbf.setNamespaceAware( true );
				//dbf.setValidating(true);
				db = dbf.newDocumentBuilder();
				//rootElement.setAttribute("caption", caption);
			} catch( Exception e ) {
				e.printStackTrace();
			}
			document = db.newDocument();
			System.out.println( " document=" + document );

			rootElement = document.createElement( "xs:schema" );

			rootElement.setAttribute( "xmlns:xs", NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			//rootElement.setAttribute("xmlns:tns",tns);
			rootElement.setAttribute( "targetNamespace", NameSpacesEnum.TNS.getNameSpaceURI() );
//--------------------------------------------------------------------------------------
			Element elementReq = document.createElement( "xs:element" );
			//elementReq.setNodeValue("nodeValue");
			elementReq.setAttribute( "name", opName );

			Element cTypeReq = document.createElement( "xs:complexType" );
			//cTypeReq..setAttibute( );
			Element sequenceReq = document.createElement( "xs:sequence" );
			Element elementReqSub = document.createElement( "xs:element" );
			elementReqSub.setAttribute( "minOccurs", "1" );
			elementReqSub.setAttribute( "maxOccurs", "1" );
			elementReqSub.setAttribute( "name", "xs:" + subTypeNameReq );
			elementReqSub.setAttribute( "type", "xs:" + subTypeNameReqType );

			sequenceReq.appendChild( elementReqSub );
			cTypeReq.appendChild( sequenceReq );
			//sequenceReq.setAttibute( );
			elementReq.appendChild( cTypeReq );
//---------------------------------------------------------------------------------------
			Element elementResp = document.createElement( "xs:element" );
			elementResp.setAttribute( "name", opName + "Response" );
			Element cTypeResp = document.createElement( "xs:complexType" );
			Element sequenceResp = document.createElement( "xs:sequence" );
			Element elementRespSub = document.createElement( "xs:element" );
			elementRespSub.setAttribute( "minOccurs", "1" );
			elementRespSub.setAttribute( "maxOccurs", "1" );
			elementRespSub.setAttribute( "name", "xs:" + subTypeNameResp );
			elementRespSub.setAttribute( "type", "xs:" + subTypeNameRespType );

			sequenceResp.appendChild( elementRespSub );
			cTypeResp.appendChild( sequenceResp );
			elementResp.appendChild( cTypeResp );

			System.out.println( " document=" + document );
			//elementReq.appendChild(cTypeReq).appendChild(sequenceReq).appendChild(elementResp).appendChild(elementFault);
			rootElement.appendChild( elementReq ).appendChild( elementResp );

			typesExt.setElement( rootElement );
			types.addExtensibilityElement( typesExt );
			//types.setDocumentationElement(null);
			//System.out.println( " types=" + types );
			localDef.setTypes( types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
	}
//	public static void main(String[] args){
//
//
//	}
}
