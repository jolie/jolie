/**
 * *************************************************************************
 * Copyright (C) 2006-09-10 by Claudio Guidi and Francesco Bullini
 * <cguidi@italianasoftware.com> <fbullini@italianasoftware.com> * * This program is free software; you can redistribute it and/or modify *
 * it under the terms of the GNU Library General Public License as * published by the Free Software Foundation; either version 2 of the *
 * License, or (at your option) any later version. * * This program is distributed in the hope that it will be useful, * but WITHOUT ANY
 * WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the * GNU General Public
 * License for more details. * * You should have received a copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA. * * For details about the
 * authors of this software, see the AUTHORS file. * *************************************************************************
 */
package joliex.wsdl;

import com.ibm.wsdl.PortTypeImpl;
import com.ibm.wsdl.ServiceImpl;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Francesco Bullini and Claudio Guidi
 */
public class WSDLDocCreator {
	// Schema

	private Document schemaDocument;
	private Element schemaRootElement;
	private static final int MAX_CARD = Integer.MAX_VALUE;
	private String tns;
	private String tnsSchema;
	private static final String TNS_SCHEMA_PREFIX = "sch";
	static ExtensionRegistry extensionRegistry;
	private static WSDLFactory wsdlFactory;
	private Definition localDef = null;
	private final List< String > rootTypes = new ArrayList<>();
	private final ProgramInspector inspector;
	private final URI originalFile;

	public WSDLDocCreator( ProgramInspector inspector, URI originalFile ) {
		this.inspector = inspector;
		this.originalFile = originalFile;
	}

	public Definition initWsdl( String serviceName, String filename ) {
		try {
			wsdlFactory = WSDLFactory.newInstance();
			localDef = wsdlFactory.newDefinition();
			extensionRegistry = wsdlFactory.newPopulatedExtensionRegistry();
			if( serviceName != null ) {
				QName servDefQN = new QName( serviceName );
				localDef.setQName( servDefQN );
			}
			localDef.addNamespace( NameSpacesEnum.WSDL.getNameSpacePrefix(), NameSpacesEnum.WSDL.getNameSpaceURI() );
			localDef.addNamespace( NameSpacesEnum.SOAP.getNameSpacePrefix(), NameSpacesEnum.SOAP.getNameSpaceURI() );
			localDef.addNamespace( "tns", tns );
			localDef.addNamespace( NameSpacesEnum.XML_SCH.getNameSpacePrefix(),
				NameSpacesEnum.XML_SCH.getNameSpaceURI() );
			localDef.setTargetNamespace( tns );
			localDef.addNamespace( "xsd1", tnsSchema );
		} catch( WSDLException ex ) {
			Logger.getLogger( WSDLDocCreator.class.getName() ).log( Level.SEVERE, null, ex );
		}

		return localDef;
	}

	public void ConvertDocument( String filename, String tns, String portName, String location ) {
		System.out.println( "Starting conversion..." );

		this.tns = tns + ".wsdl";
		this.tnsSchema = tns + ".xsd";

		initWsdl( null, filename );

		try(
			Writer w = (filename != null) ? new FileWriter( filename ) : new OutputStreamWriter( System.out );
			Writer fw = new BufferedWriter( w ) ) {
			schemaDocument = this.createDOMdocument();
			schemaRootElement = this.createSchemaRootElement( schemaDocument );

			// scans inputPorts
			OutputPortInfo[] outputPorts = inspector.getOutputPorts();
			InputPortInfo[] inputPortList = inspector.getInputPorts( originalFile );

			for( InputPortInfo inputPort : inputPortList ) {
				if( inputPort.id().equals( portName ) ) {

					// portType creation
					String portTypeName = inputPort.id();
					PortType pt = createPortType( localDef, portTypeName );

					// binding creation
					Binding bd = createBindingSOAP( localDef, pt, portTypeName + "SOAPBinding" );

					// service creation
					String address;
					if( location.isEmpty() ) {
						if( inputPort.location().toString().equals( "local" ) ) {
							address = "local";
						} else {
							address = inputPort.location().toString().substring( 6 ); // exclude socket word
							address = "http" + address;
						}
					} else {
						address = location;
					}
					createService( localDef, portTypeName + "Service", bd, address );

					// scan aggregated ports
					for( int x = 0; x < inputPort.aggregationList().length; x++ ) {
						int i = 0;
						while( !inputPort.aggregationList()[ x ].outputPortList()[ 0 ]
							.equals( outputPorts[ i ].id() ) ) {
							i++;
						}
						for( InterfaceDefinition interfaceDefinition : outputPorts[ i ].getInterfaceList() ) {

							for( Entry< String, OperationDeclaration > entry : interfaceDefinition.operationsMap()
								.entrySet() ) {
								addOperation2WSDL( entry.getValue(), pt, bd );
							}
						}
					}

					// scans port interfaces
					for( InterfaceDefinition interfaceDefinition : inputPort.getInterfaceList() ) {
						// scan operations
						Map< String, OperationDeclaration > operationMap = interfaceDefinition.operationsMap();

						for( Entry< String, OperationDeclaration > entry : operationMap.entrySet() ) {
							addOperation2WSDL( entry.getValue(), pt, bd );
						}

					}
				}

			}

			setSchemaDocIntoWSDLTypes( schemaDocument );
			WSDLWriter wsdl_writer = wsdlFactory.newWSDLWriter();
			wsdl_writer.writeWSDL( localDef, fw );
		} catch( IOException | WSDLException e ) {
			System.err.println( e.getMessage() );
			Logger.getLogger( WSDLDocCreator.class.getName() ).log( Level.SEVERE, null, e );
		}
		System.out.println( "Success: WSDL document generated!" );
	}

	private void addOperation2WSDL( OperationDeclaration operationDeclaration, PortType pt, Binding bd ) {
		if( operationDeclaration instanceof OneWayOperationDeclaration ) {
			// OW

			// -------------- adding operation
			OneWayOperationDeclaration oneWayOperation = (OneWayOperationDeclaration) operationDeclaration;
			Operation wsdlOp = addOWOperation2PT( localDef, pt, oneWayOperation );

			// adding operation binding
			addOperationSOAPBinding( localDef, wsdlOp, bd );

		} else {
			// RR
			// -------------- adding operation
			RequestResponseOperationDeclaration requestResponseOperation =
				(RequestResponseOperationDeclaration) operationDeclaration;
			Operation wsdlOp = addRROperation2PT( localDef, pt, requestResponseOperation );

			// adding operation binding
			addOperationSOAPBinding( localDef, wsdlOp, bd );

		}
	}

	private String getSchemaNativeType( NativeType nType ) {

		/*
		 * TO DO: wsdl_types ANY, RAW, VOID
		 */
		String prefix = "xs:";
		String suffix = "";
		if( nType.equals( NativeType.STRING ) ) {
			suffix = "string";
		} else if( nType.equals( NativeType.DOUBLE ) ) {
			suffix = "double";
		} else if( nType.equals( NativeType.INT ) ) {
			suffix = "int";
		} else if( nType.equals( NativeType.BOOL ) ) {
			suffix = "boolean";
		} else if( nType.equals( NativeType.LONG ) ) {
			suffix = "long";
		}
		if( suffix.isEmpty() ) {
			return "";
		} else {
			return prefix + suffix;
		}
	}

	private void addRootType( TypeDefinition type ) throws Exception {
		if( type instanceof TypeDefinitionLink ) {
			throw (new Exception( "ERROR, type " + type.name()
				+ ":conversion not allowed when the types defined as operation messages are linked type!" ));
		}
		if( !rootTypes.contains( type.name() ) ) {
			schemaRootElement.appendChild( createTypeDefinition( (TypeInlineDefinition) type, false ) );
		}
		rootTypes.add( type.name() );
	}

	private Element createTypeDefinition( TypeInlineDefinition type, boolean inMessage ) throws Exception {
		if( type.basicType().nativeType() != NativeType.VOID ) {
			throw (new Exception( "ERROR, type " + type.name()
				+ ": conversion not allowed when the types defined as operation messages have native type different from void!" ));
		}

		Element newEl = schemaDocument.createElement( "xs:complexType" );
		if( inMessage == false ) {
			String typename = type.name();
			newEl.setAttribute( "name", typename );
		}
		Element sequence = schemaDocument.createElement( "xs:sequence" );

		// adding subtypes
		if( type.hasSubTypes() ) {
			for( Entry< String, TypeDefinition > stringTypeDefinitionEntry : type.subTypes() ) {
				TypeDefinition curType = stringTypeDefinitionEntry.getValue();
				Element subEl = schemaDocument.createElement( "xs:element" );
				subEl.setAttribute( "name", curType.name() );
				subEl.setAttribute( "minOccurs", Integer.toString( curType.cardinality().min() ) );
				String maxOccurs = "unbounded";
				if( curType.cardinality().max() < MAX_CARD ) {
					maxOccurs = Integer.toString( curType.cardinality().max() );
				}
				subEl.setAttribute( "maxOccurs", maxOccurs );
				if( curType instanceof TypeInlineDefinition ) {
					if( ((TypeInlineDefinition) curType).hasSubTypes() ) {
						if( ((TypeInlineDefinition) curType).basicType().nativeType() != NativeType.VOID ) {
							throw (new Exception( "ERROR, type " + curType.name()
								+ ": conversion not allowed when the types defined as operation messages have native type different from void!" ));
						} else {
							subEl.appendChild( createTypeDefinition( (TypeInlineDefinition) curType, true ) );
						}
					} else {
						subEl.setAttribute( "type",
							getSchemaNativeType( ((TypeInlineDefinition) curType).basicType().nativeType() ) );
					}
				} else if( curType instanceof TypeDefinitionLink ) {
					subEl.setAttribute( "type",
						TNS_SCHEMA_PREFIX + ":" + ((TypeDefinitionLink) curType).linkedTypeName() );
					addRootType( ((TypeDefinitionLink) curType).linkedType() );
				}
				sequence.appendChild( subEl );
			}
		}
		newEl.appendChild( sequence );
		return newEl;
	}

	private void addMessageType( TypeDefinition rootType, String typename ) throws Exception {
		// when converting from Jolie type of messages must have root type = "void"
		// no type link are allowed for conversion
		// message types define elements
		if( !rootTypes.contains( rootType.name() ) ) {
			Element newEl = schemaDocument.createElement( "xs:element" );
			newEl.setAttribute( "name", typename );
			if( rootType instanceof TypeInlineDefinition ) {
				newEl.appendChild( createTypeDefinition( (TypeInlineDefinition) rootType, true ) );
				rootTypes.add( typename );
				schemaRootElement.appendChild( newEl );
				if( ((TypeInlineDefinition) rootType).basicType().nativeType() != NativeType.VOID ) {
					throw (new Exception( "ERROR, type " + rootType.name()
						+ ": conversion not allowed when the types defined as operation messages have native type different from void!" ));
				}
			} else if( rootType instanceof TypeDefinitionLink ) {
				throw (new Exception( "ERROR, type " + rootType.name()
					+ ":conversion not allowed when the types defined as operation messages are linked type!" ));
				// newEl.appendChild( lookForLinkedType( (TypeDefinitionLink ) rootType, typename ));
				// schemaRootElement.appendChild( createTypeDefinitionLink( ( TypeDefinitionLink ) rootType, true,
				// typename ));
			} else if( rootType instanceof TypeChoiceDefinition ) {
				throw (new Exception( "ERROR, type " + rootType.name()
					+ ":conversion not allowed when the types defined as operation messages are choice types!" ));
			}
		}

	}

	private Message addRequestMessage( Definition localDef, OperationDeclaration op ) {

		Message inputMessage = localDef.createMessage();
		inputMessage.setUndefined( false );

		Part inputPart = localDef.createPart();
		inputPart.setName( "body" );
		try {
			// adding wsdl_types related to this message
			if( op instanceof OneWayOperationDeclaration ) {
				OneWayOperationDeclaration op_ow = (OneWayOperationDeclaration) op;

				// set the message name as the name of the jolie request message type
				inputMessage.setQName( new QName( tns, op_ow.requestType().name() ) );
				addMessageType( op_ow.requestType(), op_ow.id() );

			} else {
				RequestResponseOperationDeclaration op_rr = (RequestResponseOperationDeclaration) op;
				// set the message name as the name of the jolie request message type
				inputMessage.setQName( new QName( tns, op_rr.requestType().name() ) );
				addMessageType( op_rr.requestType(), op_rr.id() );

			}
			// set the input part as the operation name
			inputPart.setElementName( new QName( tnsSchema, op.id() ) );

			inputMessage.addPart( inputPart );
			inputMessage.setUndefined( false );

			localDef.addMessage( inputMessage );
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return inputMessage;
	}

	private Message addResponseMessage( Definition localDef, OperationDeclaration op ) {

		Message outputMessage = localDef.createMessage();
		outputMessage.setUndefined( false );

		Part outputPart = localDef.createPart();
		outputPart.setName( "body" );

		// adding wsdl_types related to this message
		try {
			RequestResponseOperationDeclaration op_rr = (RequestResponseOperationDeclaration) op;
			String outputPartName = op_rr.id() + "Response";
			// set the message name as the name of the jolie response message type
			outputMessage.setQName( new QName( tns, op_rr.responseType().name() ) );
			addMessageType( op_rr.responseType(), outputPartName );

			outputPart.setElementName( new QName( tnsSchema, outputPartName ) );

			outputMessage.addPart( outputPart );
			outputMessage.setUndefined( false );

			localDef.addMessage( outputMessage );
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return outputMessage;

	}

	private Message addFaultMessage( Definition localDef, TypeDefinition tp ) {
		Message faultMessage = localDef.createMessage();
		faultMessage.setUndefined( false );

		// set the fault message name as the name of the fault jolie message type
		faultMessage.setQName( new QName( tns, tp.name() ) );

		Part faultPart = localDef.createPart();
		faultPart.setName( "body" );

		String faultPartName = tp.name();

		try {
			// adding wsdl_types related to this message
			addMessageType( tp, faultPartName );

			faultPart.setElementName( new QName( tnsSchema, faultPartName ) );
			faultMessage.addPart( faultPart );
			faultMessage.setUndefined( false );

			localDef.addMessage( faultMessage );
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return faultMessage;

	}

	private PortType createPortType( Definition def, String portTypeName ) {
		PortType pt = def.getPortType( new QName( portTypeName ) );
		if( pt == null ) {
			pt = new PortTypeImpl();
		}

		pt.setUndefined( false );
		QName pt_QN = new QName( tns, portTypeName );

		pt.setQName( pt_QN );
		def.addPortType( pt );

		return pt;
	}

	private Operation addOWOperation2PT( Definition def, PortType pt, OneWayOperationDeclaration op ) {
		Operation wsdlOp = def.createOperation();

		wsdlOp.setName( op.id() );
		wsdlOp.setStyle( OperationType.ONE_WAY );
		wsdlOp.setUndefined( false );

		Input in = def.createInput();
		Message msg_req = addRequestMessage( localDef, op );
		msg_req.setUndefined( false );
		in.setMessage( msg_req );
		wsdlOp.setInput( in );
		wsdlOp.setUndefined( false );

		pt.addOperation( wsdlOp );

		return wsdlOp;
	}

	private Operation addRROperation2PT( Definition def, PortType pt, RequestResponseOperationDeclaration op ) {
		Operation wsdlOp = def.createOperation();

		wsdlOp.setName( op.id() );
		wsdlOp.setStyle( OperationType.REQUEST_RESPONSE );
		wsdlOp.setUndefined( false );

		// creating input
		Input in = def.createInput();
		Message msg_req = addRequestMessage( localDef, op );
		in.setMessage( msg_req );
		wsdlOp.setInput( in );

		// creating output
		Output out = def.createOutput();
		Message msg_resp = addResponseMessage( localDef, op );
		out.setMessage( msg_resp );
		wsdlOp.setOutput( out );

		// creating faults
		for( Entry< String, TypeDefinition > curFault : op.faults().entrySet() ) {
			Fault fault = localDef.createFault();
			fault.setName( curFault.getKey() );
			Message flt_msg = addFaultMessage( localDef, curFault.getValue() );
			fault.setMessage( flt_msg );
			wsdlOp.addFault( fault );

		}
		pt.addOperation( wsdlOp );

		return wsdlOp;
	}

	private Binding createBindingSOAP( Definition def, PortType pt, String bindingName ) {
		Binding bind = def.getBinding( new QName( bindingName ) );
		if( bind == null ) {
			bind = def.createBinding();
			bind.setQName( new QName( tns, bindingName ) );
		}
		bind.setPortType( pt );
		bind.setUndefined( false );
		try {
			SOAPBinding soapBinding = (SOAPBinding) extensionRegistry.createExtension( Binding.class,
				new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "binding" ) );
			soapBinding.setTransportURI( NameSpacesEnum.SOAP_OVER_HTTP.getNameSpaceURI() );
			soapBinding.setStyle( "document" );
			bind.addExtensibilityElement( soapBinding );
		} catch( WSDLException ex ) {
			System.err.println( ex.getMessage() );
		}
		def.addBinding( bind );

		return bind;

	}

	private void addOperationSOAPBinding( Definition localDef, Operation wsdlOp, Binding bind ) {
		try {
			// creating operation binding
			BindingOperation bindOp = localDef.createBindingOperation();
			bindOp.setName( wsdlOp.getName() );

			// adding soap extensibility elements
			SOAPOperation soapOperation = (SOAPOperation) extensionRegistry.createExtension( BindingOperation.class,
				new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "operation" ) );
			soapOperation.setStyle( "document" );
			// NOTA-BENE: Come settare SOAPACTION? jolie usa SOAP1.1 o 1.2? COme usa la SoapAction?
			soapOperation.setSoapActionURI( wsdlOp.getName() );
			bindOp.addExtensibilityElement( soapOperation );
			bindOp.setOperation( wsdlOp );

			// adding input
			BindingInput bindingInput = localDef.createBindingInput();
			SOAPBody body = (SOAPBody) extensionRegistry.createExtension( BindingInput.class,
				new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "body" ) );
			body.setUse( "literal" );
			bindingInput.addExtensibilityElement( body );
			bindOp.setBindingInput( bindingInput );

			// adding output
			BindingOutput bindingOutput = localDef.createBindingOutput();
			bindingOutput.addExtensibilityElement( body );
			bindOp.setBindingOutput( bindingOutput );

			// adding fault
			if( !wsdlOp.getFaults().isEmpty() ) {
				for( Object o : wsdlOp.getFaults().keySet() ) {
					BindingFault bindingFault = localDef.createBindingFault();
					SOAPFault soapFault = (SOAPFault) extensionRegistry.createExtension( BindingFault.class,
						new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "fault" ) );
					soapFault.setUse( "literal" );
					String faultName = o.toString();
					bindingFault.setName( faultName );
					soapFault.setName( faultName );
					bindingFault.addExtensibilityElement( soapFault );
					bindOp.addBindingFault( bindingFault );
				}
			}

			bind.addBindingOperation( bindOp );

		} catch( WSDLException ex ) {
			ex.printStackTrace();
		}

	}

	public Service createService( Definition localdef, String serviceName, Binding bind, String mySOAPAddress ) {
		Port p = localDef.createPort();
		p.setName( serviceName + "Port" );
		try {

			SOAPAddress soapAddress = (SOAPAddress) extensionRegistry.createExtension( Port.class,
				new QName( NameSpacesEnum.SOAP.getNameSpaceURI(), "address" ) );
			soapAddress.setLocationURI( mySOAPAddress );
			p.addExtensibilityElement( soapAddress );
		} catch( WSDLException ex ) {
			ex.printStackTrace();
		}
		p.setBinding( bind );
		Service s = new ServiceImpl();
		QName serviceQName = new QName( serviceName );
		s.setQName( serviceQName );
		s.addPort( p );
		localDef.addService( s );
		return s;
	}

	private Document createDOMdocument() {
		Document document = null;
		DocumentBuilder db;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware( true );
			db = dbf.newDocumentBuilder();
			document = db.newDocument();
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return document;
	}

	private Element createSchemaRootElement( Document document ) {
		Element rootElement = document.createElement( "xs:schema" );
		rootElement.setAttribute( "xmlns:xs", NameSpacesEnum.XML_SCH.getNameSpaceURI() );
		rootElement.setAttribute( "targetNamespace", tnsSchema );
		rootElement.setAttribute( "xmlns:" + TNS_SCHEMA_PREFIX, tnsSchema );
		document.appendChild( rootElement );
		return rootElement;

	}

	public void setSchemaDocIntoWSDLTypes( Document doc ) {
		try {

			Types wsdl_types = localDef.createTypes();
			Schema typesExt = (Schema) extensionRegistry.createExtension( Types.class,
				new QName( NameSpacesEnum.XML_SCH.getNameSpaceURI(), "schema" ) );
			typesExt.setElement( (Element) doc.getFirstChild() );
			wsdl_types.addExtensibilityElement( typesExt );
			localDef.setTypes( wsdl_types );

		} catch( Exception ex ) {
			System.err.println( ex.getMessage() );
		}
	}
}
