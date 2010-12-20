/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.doc.impl.html;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.util.ProgramInspector;

/**
 *
 * @author balint
 */
public class HtmlDocumentCreatorNew
{
	private Writer writer;
	private List<String> filesList;
	private HashMap<String, String> HyperlinkMap;
	private URI[] filesList1;
	private ProgramInspector inspector;
	private String nameFile;
	private String directorySourceFile;
	private String directorySOA;

	public HtmlDocumentCreatorNew( ProgramInspector inspector, String directorySourceFile )
	{
		this.inspector = inspector;
		this.directorySourceFile = directorySourceFile;
	}

	public void ConvertDocument()
	{
		filesList1 = inspector.getSources();
		//filesList1[0]
		InputPortInfo[] inputPortInfoList = inspector.getInputPorts();
		OutputPortInfo[] outputPortInfoList = inspector.getOutputPorts( null );
		InterfaceDefinition[] interfacesList = inspector.getInterfaces();
		TypeDefinition[] typesList = inspector.getTypes();
		HyperlinkMap = new HashMap<String, String>();
		directorySOA = directorySourceFile.substring( 0, directorySourceFile.lastIndexOf( "/" ) + 1 );
		System.out.print( directorySOA + "\n" );
		System.out.print( filesList1.length + "\n" );
		for( URI supportURI : filesList1 ) {
			System.out.print( "the schema is:" + supportURI.getScheme() + "\n" );
			System.out.print( "the path is:" + supportURI.getPath() + "\n" );
			if ( supportURI.getPath() != null ) {
				if ( (supportURI.getPath().contains( directorySOA ) && (supportURI.getScheme().equals( "file" ))) ) {

					outputPortInfoList = inspector.getOutputPorts( supportURI );
					interfacesList = inspector.getInterfaces( supportURI );
					typesList = inspector.getTypes( supportURI );
					nameFile = ((supportURI.getPath().substring( supportURI.getPath().lastIndexOf( "/" ) + 1 )));
					String nameFileBuffer = ((supportURI.getPath().substring( supportURI.getPath().lastIndexOf( "/" ) + 1 )).replace( ".", "_" )) + ".html";

					//System.out.print( nameFile + "\n" );
					try {
						writer = new BufferedWriter( new FileWriter( nameFileBuffer ) );
					} catch( IOException ex ) {
						Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
					}
					for( OutputPortInfo output : outputPortInfoList ) {
						try {
							ConvertOutputPorts( output, writer );
						} catch( IOException ex ) {
							Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
						}
					}
					for( InterfaceDefinition interfaceDefiniton : interfacesList ) {
						try {
							ConvertInterface( interfaceDefiniton, writer );
						} catch( IOException ex ) {
							Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
						}

					}
					for( TypeDefinition typeDefinition : typesList ) {
						try {
							ConvertTypes( typeDefinition, writer );
						} catch( IOException ex ) {
							Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
						}



					}
				}

				try {
					writer.flush();
				} catch( IOException ex ) {
					Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
				}
				try {
					writer.close();
				} catch( IOException ex ) {
					Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
				}
			} else {
				if ( supportURI.getSchemeSpecificPart() != null ) {
					//here all the processing for the Olsyntax node
					outputPortInfoList = inspector.getOutputPorts( supportURI );
					interfacesList = inspector.getInterfaces( supportURI );
					typesList = inspector.getTypes( supportURI );
					nameFile = supportURI.getSchemeSpecificPart();
					String nameFileBuffer = (supportURI.getSchemeSpecificPart().replace( ".", "_" )) + ".html";

					//System.out.print( nameFile + "\n" );
					try {
						writer = new BufferedWriter( new FileWriter( nameFileBuffer ) );
					} catch( IOException ex ) {
						Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
					}
					for( OutputPortInfo output : outputPortInfoList ) {
						try {
							ConvertOutputPorts( output, writer );
						} catch( IOException ex ) {
							Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
						}
					}
					for( InterfaceDefinition interfaceDefiniton : interfacesList ) {
						try {
							ConvertInterface( interfaceDefiniton, writer );
						} catch( IOException ex ) {
							Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
						}

					}
					for( TypeDefinition typeDefinition : typesList ) {
						try {
							ConvertTypes( typeDefinition, writer );
						} catch( IOException ex ) {
							Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
						}



					}
				}

				try {
					writer.flush();
				} catch( IOException ex ) {
					Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
				}
				try {
					writer.close();
				} catch( IOException ex ) {
					Logger.getLogger( HtmlDocumentCreatorNew.class.getName() ).log( Level.SEVERE, null, ex );
				}







			}
		}



	}

	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException
	{
		writer.write( "<H3>" + "Interface definition " + interfaceDefinition.name() + "</H3>" );
		writer.write( "<a name=\"" + interfaceDefinition.name() + "\"></a>" );
		if ( !(interfaceDefinition.getDocumentation() == null) ) {
			writer.write( interfaceDefinition.getDocumentation() );
		}
		OperationDeclaration operation;
		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>Heading</th>" );
		writer.write( "<th>Input type</th>" );
		writer.write( "<th>Output type</th>" );
		writer.write( "</tr>" );
		for( Entry<String, OperationDeclaration> entry :
			interfaceDefinition.operationsMap().entrySet() ) {
			if ( interfaceDefinition.getDocumentation() != null ) {
				writer.write( interfaceDefinition.getDocumentation() );
			}
			operation = entry.getValue(); // questo è l'oggetto operationdeclaration

			writer.write( "<tr>" );
			writer.write( "<td>" + operation.id() + "</td>" );
			//writer.write( "<td>" + "<a href=\"#Code\"> Code </a><br />" + "</td>" + NEWLINE );

			if ( operation instanceof RequestResponseOperationDeclaration ) {


				String supportFileName = (((RequestResponseOperationDeclaration) operation).requestType().context().sourceName()).substring( ((RequestResponseOperationDeclaration) operation).requestType().context().sourceName().lastIndexOf( "/" ) + 1 );
				String supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
				supportHyperLinkFileName += ".html";
				if (((RequestResponseOperationDeclaration) operation).requestType().hasSubTypes()){

				if ( !(supportFileName.equals( this.nameFile )) ) {
					writer.write( "<td>" + "<a href=\"" + supportHyperLinkFileName + "\"" + "target=\"" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
				} else {

					writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );

				}
				}else
				{
				  					writer.write( "<td>" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "<<br />" + "</td>" );



				}


                if (((RequestResponseOperationDeclaration) operation).responseType().hasSubTypes()){

				supportFileName = (((RequestResponseOperationDeclaration) operation).responseType().context().sourceName().substring( ((RequestResponseOperationDeclaration) operation).responseType().context().sourceName().indexOf( "/" ) + 1 ));
				supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
				System.out.print( ((RequestResponseOperationDeclaration) operation).responseType().id() + "  il file e :" + supportHyperLinkFileName + "\n" );
				//System.in.read();
				supportHyperLinkFileName += ".html";
				if ( supportFileName.equals( this.nameFile ) ) {
					writer.write( "<td>" + "<a href=\"" + supportHyperLinkFileName + "\"" + "target=\"" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );
				} else {

					writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );

				}
				}else
				{

				writer.write( "<td>" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "<br />" + "</td>" );


				}

				//writer.write( "<td>"+"<a href=\""+supportHyperLinkFileName+"\""+"target=\""+((RequestResponseOperationDeclaration) operation).responseType().id()+"\">"+((RequestResponseOperationDeclaration) operation).responseType().id()+"</a><br />"+ "</td>");
				writer.write( "</tr>" );
			}
			if ( operation instanceof OneWayOperationDeclaration ) {


				writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
				writer.write( "</tr>" );
			}
		}
		writer.write( "</table>" );
	}

	public void ConvertOutputPorts( OutputPortInfo outputPortInfo, Writer writer )
		throws IOException
	{

		String location = outputPortInfo.location() == null ? "" : outputPortInfo.location().toString();
		writer.write( "<H3>" + "Documentation for Port " + outputPortInfo.id() + "</H3>" );

		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>Port Name</th>" );
		writer.write( "<th>Location</th>" );
		writer.write( "<th>Protocol ID</th>" );
		writer.write( "<th>Code</th>" );
		writer.write( "</tr>" );
		writer.write( "<tr>" );
		writer.write( "<td>" + outputPortInfo.id() + "</td>" );
		writer.write( "<td>" + location + "</td>" );
		writer.write( "<td>" + outputPortInfo.protocolId() + "</td>" );
		writer.write( "<td>" + "<a href=\"#Code\"> CodePort </a><br />" + "</td>" + "</BR>" );
		writer.write( "</tr>" );

		if ( outputPortInfo.getDocumentation() != null ) {
			writer.write( outputPortInfo.getDocumentation() );
		}
		List<InterfaceDefinition> interfacesList = outputPortInfo.getInterfaceList();
		writer.write( "<tr>" );
		writer.write( "</tr>" );
		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>InterfaceName</th>" );
		writer.write( "</tr>" );
		for( InterfaceDefinition interfaceDefintion : interfacesList ) {

			String interfaceSourceName = interfaceDefintion.context().sourceName().substring( interfaceDefintion.context().sourceName().lastIndexOf( "/" ) + 1 );
			if ( interfaceSourceName.equals( nameFile ) ) {
				writer.write( "<tr>" );
				writer.write( "<td>" + "<A href=\"" + interfaceDefintion.name() + "\">" + interfaceDefintion.name() + " </A></td>" );
				writer.write( "</tr>" + "<BR>" );
			}

		}
      writer.write( "</table>" );
	}

	public void ConvertInputPorts( InputPortInfo inputPortInfo, Writer writer )
		throws IOException
	{


		writer.write( "<H3>" + "Documentation for Port " + inputPortInfo.id() + "</H3>" );

		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>Port Name</th>" );
		writer.write( "<th>Location</th>" );
		writer.write( "<th>Protocol ID</th>" );
		//writer.write( "<th>Code</th>" );
		writer.write( "</tr>" );
		writer.write( "<tr>" );
		writer.write( "<td>" + inputPortInfo.id() + "</td>" );
		writer.write( "<td>" + inputPortInfo.id() + "</td>" );
		writer.write( "<td>" + inputPortInfo.id() + "</td>" );
		//writer.write( "<td>" + "<a href=\"#Code\"> CodePort </a><br />" + "</td>" + "<BR>" );
		writer.write( "</tr>" );

		if ( inputPortInfo.getDocumentation() != null ) {
			writer.write( inputPortInfo.getDocumentation() );
		}
		List<InterfaceDefinition> interfacesListLocal = inputPortInfo.getInterfaceList();
		writer.write( "<tr>" );
		writer.write( "</tr>" );
		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>InterfaceName</th>" );
		writer.write( "</tr>" );
		for( int counterInterfaces = 0; counterInterfaces < interfacesListLocal.size(); counterInterfaces++ ) {
			writer.write( "<tr>" );
			writer.write( "<td>" + "<A href=\"" + HyperlinkMap.get( interfacesListLocal.get( counterInterfaces ) ) + "\">" + interfacesListLocal.get( counterInterfaces ) + " </A></td>" );
			writer.write( "</tr>" + "<BR>" );
		}

	}

	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertTypes( TypeDefinition typesDefinition, Writer writer )
		throws IOException
	{


		if ( typesDefinition.hasSubTypes() ) {

			writer.write( "<H3>" + "Type " + typesDefinition.id() + "</H3>" );
			writer.write( "<a name=\"" + typesDefinition.id() + "\"></a>" );
			writer.write( "\n" );
			writeType( typesDefinition, false, writer, 0 );
		}


	}

	private void writeType( TypeDefinition type, boolean subType, Writer writer, int indetationLevel )
		throws IOException
	{
		StringBuilder builder = new StringBuilder();
		if ( subType ) {
			builder.append( "<BR>" );
			for( int indexIndetation = 0; indexIndetation < indetationLevel + 1; indexIndetation++ ) {

				builder.append( "&nbsp" );

			}
			builder.append( "." );
		} else {
			builder.append( "type " );
		}
		builder.append( type.id() ).append( getCardinalityString( type ) ).append( ':' );
		Iterator<Entry<String, TypeDefinition>> iterator;
		Entry<String, TypeDefinition> entry;

		if ( type instanceof TypeDefinitionLink ) {
			TypeDefinitionLink link = (TypeDefinitionLink) type;
			//here goese the check
			link.linkedType().context().sourceName();
			System.out.print( link.linkedType().context().source().getRawSchemeSpecificPart() );
			String supportLinkedTypeFileName = link.linkedType().context().source().getSchemeSpecificPart();
			if ( supportLinkedTypeFileName.equals( this.nameFile ) ) {
				builder.append( "<a href=\"#" + link.linkedType().id() + "\">" + link.linkedType().id() + "</a>" );
			} else {

				String supportHyperLinkFileName = supportLinkedTypeFileName.replace( ".", "_" ) + ".html";
				builder.append( "<a href=\"" + supportHyperLinkFileName + "\"" + "target=\"" + link.linkedType().id() + "\">" + link.linkedType().id() + "</a>" );

			}
			if ( subType == false ) {
//				builder.append( "<BR>" );
//
//				for( int indexIndetation = 0; indexIndetation < indetationLevel + 1; indexIndetation++ ) {
//					builder.append( "&nbsp" );
//				}
//				builder.append( "}" );
			}
			writer.write( builder.toString() );
			//writer.write( "<BR>" );
		} else if ( type.untypedSubTypes() ) {
			builder.append( "undefined" );
			writer.write( builder.toString() );
		} else {
			builder.append( nativeTypeToString( type.nativeType() ) );
			if ( type.hasSubTypes() ) {
				builder.append( "<BR>" );

				for( int indexIndetation = 0; indexIndetation < indetationLevel + 1; indexIndetation++ ) {
					builder.append( "&nbsp" );
				}
				builder.append( "{" );
				//builder.append( "<BR>" );
			}
			writer.write( builder.toString() );
			if ( type.hasSubTypes() ) {
				//indent();
				for(int indexEntry=type.subTypes().toArray().length-1;indexEntry>=0;indexEntry--) {
					
					entry=(Entry<String,TypeDefinition>)((type.subTypes().toArray())[indexEntry]);
					writeType( entry.getValue(), true, writer, indetationLevel + 2 );
				}
				//unindent();
			}

			if ( type.hasSubTypes() ) {

				writer.write( "<BR>" );

				for( int indexIndetation = 0; indexIndetation < indetationLevel + 1; indexIndetation++ ) {
					writer.write( "&nbsp" );
				}
				writer.write( "}" );



			}
			if ( subType == false ) {
				
			}
		}
	}

	private static String nativeTypeToString( NativeType nativeType )
	{
		return (nativeType == null) ? "" : nativeType.id();
	}

	private String getCardinalityString( TypeDefinition type )
	{
		if ( type.cardinality().equals( Constants.RANGE_ONE_TO_ONE ) ) {
			return "";
		} else if ( type.cardinality().min() == 0 && type.cardinality().max() == 1 ) {
			return "?";
		} else if ( type.cardinality().min() == 0 && type.cardinality().max() == Integer.MAX_VALUE ) {
			return "*";
		} else {
			return new StringBuilder().append( '[' ).append( type.cardinality().min() ).append( ',' ).append( type.cardinality().max() ).append( ']' ).toString();
		}
	}
}
