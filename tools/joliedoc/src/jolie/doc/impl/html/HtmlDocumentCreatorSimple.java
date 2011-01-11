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
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;
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
public class HtmlDocumentCreatorSimple
{
	private Writer writer;
	private List<String> filesList;
	private HashMap<String, String> HyperlinkMap;
	private URI[] filesList1;
	private ProgramInspector inspector;
	private String nameFile;
	private Vector<TypeDefintionRecursive> typesList;
	private URI directorySourceFile;
	private String directorySOA;
	private JolieDocFIleWriter jolieDocWriter;

	public HtmlDocumentCreatorSimple( ProgramInspector inspector, URI directorySourceFile )
	{
		this.inspector = inspector;
		this.directorySourceFile = directorySourceFile;
	}

	public void ConvertDocument()
		throws IOException
	{
		filesList1 = inspector.getSources();
		directorySOA=directorySourceFile.getSchemeSpecificPart().substring( 0,directorySourceFile.getRawSchemeSpecificPart().lastIndexOf( "/")+1);
		String nameFileHTML=directorySourceFile.getSchemeSpecificPart().substring( directorySourceFile.getRawSchemeSpecificPart().lastIndexOf( "/")+1,directorySourceFile.getRawSchemeSpecificPart().length());
		nameFileHTML.replace( ".", "_" );
		writer = new BufferedWriter( new FileWriter( directorySOA+ nameFileHTML+".html" ) );
		jolieDocWriter = new JolieDocFIleWriter( writer );
		typesList = new Stack<TypeDefintionRecursive>();


		InputPortInfo[] inputPorts = inspector.getInputPorts( directorySourceFile );
		for( InputPortInfo inputPort : inputPorts ) {
			jolieDocWriter.addInputPort( inputPort );
			List<InterfaceDefinition> interfacesList = inputPort.getInterfaceList();
			for( InterfaceDefinition interfaceDefintion : interfacesList ) {
				jolieDocWriter.addInterface( interfaceDefintion );
			}
			for( OperationDeclaration operationDeclaration : inputPort.operations() ) {

				if ( operationDeclaration instanceof RequestResponseOperationDeclaration ) {
					TypeDefinition requestType = ((RequestResponseOperationDeclaration) operationDeclaration).requestType();
					TypeDefinition responseType = ((RequestResponseOperationDeclaration) operationDeclaration).responseType();
					if ( requestType.hasSubTypes() ) {
						jolieDocWriter.addType( requestType );
						writeType( responseType , false, jolieDocWriter );
					}
					if ( responseType.hasSubTypes() ) {
                        jolieDocWriter.addType( responseType );
						writeType( responseType , false, jolieDocWriter );

					}
					}

				}
			}

       jolieDocWriter.write();
		}
	public void ConvertInterface( InterfaceDefinition interfaceDefinition, Writer writer )
		throws IOException
	{
		writer.write( "<H3>" + "Interface definition " + interfaceDefinition.name() + "</H3>" );
		writer.write( "<a name=\"" + interfaceDefinition.name() + "\"></a>" );
		if ( !(interfaceDefinition.getDocumentation() == null) ) {
			writer.write( "<BR>" );
			writer.write( interfaceDefinition.getDocumentation() );
			writer.write( "<BR>" );
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


			if ( operation instanceof RequestResponseOperationDeclaration ) {


				String supportFileName = (((RequestResponseOperationDeclaration) operation).requestType().context().sourceName()).substring( ((RequestResponseOperationDeclaration) operation).requestType().context().sourceName().lastIndexOf( "/" ) + 1 );
				String supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
				supportHyperLinkFileName += ".html";
				if ( ((RequestResponseOperationDeclaration) operation).requestType().hasSubTypes() ) {



					writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );


				} else {
					writer.write( "<td>" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "<<br />" + "</td>" );
				}


				if ( ((RequestResponseOperationDeclaration) operation).responseType().hasSubTypes() ) {

					supportFileName = (((RequestResponseOperationDeclaration) operation).responseType().context().sourceName().substring( ((RequestResponseOperationDeclaration) operation).responseType().context().sourceName().indexOf( "/" ) + 1 ));
					supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
					System.out.print( ((RequestResponseOperationDeclaration) operation).responseType().id() + "  il file e :" + supportHyperLinkFileName + "\n" );
					//System.in.read();
					supportHyperLinkFileName += ".html";


					writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );


				} else {

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







	}

	public void ConvertOperations( OperationDeclaration operationDeclaration, Writer writer )
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void ConvertTypes( TypeDefinition typesDefinition)
		throws IOException
	{


		if ( typesDefinition.hasSubTypes() ) {
//			writer.write( "<H3 id=\""+ typesDefinition.id() +"\">"+"Type " + typesDefinition.id() + "</H3>" );
//			writer.write( "<a name=\"" + typesDefinition.id() + "\"></a>" );
//			writer.write( "\n" );
//			writeType( typesDefinition, false, writer, 0 );
		}


	}

	private void writeType( TypeDefinition type, boolean subType, JolieDocFIleWriter jolieDocFileWriter )
		throws IOException
	{
		
		
		Iterator<Entry<String, TypeDefinition>> iterator;
		Entry<String, TypeDefinition> entry;

		if ( type instanceof TypeDefinitionLink ) {
			jolieDocFileWriter.addLinkedType( (TypeDefinitionLink)type );
			writeType( ((TypeDefinitionLink)type).linkedType(), false, jolieDocFileWriter );
		} else if ( type.untypedSubTypes() ) {
			
		} else {
		
			
			if ( type.hasSubTypes() ) {
				//indent();
				for( int indexEntry = type.subTypes().toArray().length - 1; indexEntry >= 0; indexEntry-- ) {

					entry = (Entry<String, TypeDefinition>) ((type.subTypes().toArray())[indexEntry]);
					writeType( entry.getValue(), true,jolieDocFileWriter);
				}
				//unindent();
			}

//			
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
