/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.doc.impl.html;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap.SimpleEntry;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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

/**
 *
 * @author balint
 */
public class JolieDocFIleWriter {
	private Vector <InputPortInfo> inputPortVector;
	private Vector <OutputPortInfo> outPortVector;
	private Vector<String> typeDefintionNameVector;
	private Vector <TypeDefinition> typeDefinitonVector;
	private Vector <TypeDefinitionLink> typeDefinitionLinkVector;
	private Vector<String> typeDefintionLinkNameVector;
	private Vector <InterfaceDefinition> interfaceDefintionVector;
	private Vector <RequestResponseOperationDeclaration> requestResponseOperationDeclarationsVector;
	private Vector <OneWayOperationDeclaration> oneWayOperationDeclarationsVector;
	private Writer writer;
	private Iterator<Entry<String, TypeDefinition>> typesIterator;
	public JolieDocFIleWriter(Writer writer){


		this.writer=writer;
		inputPortVector= new Vector<InputPortInfo>();
		outPortVector= new Vector<OutputPortInfo>();
		typeDefinitionLinkVector= new Vector<TypeDefinitionLink>();
		typeDefintionLinkNameVector= new Vector<String>();
		typeDefinitonVector= new Vector<TypeDefinition>();
		typeDefintionNameVector= new Vector<String>();
		interfaceDefintionVector= new Vector<InterfaceDefinition>();
		requestResponseOperationDeclarationsVector= new Vector<RequestResponseOperationDeclaration>();
		oneWayOperationDeclarationsVector= new Vector<OneWayOperationDeclaration>();

	}
	public  void addInputPort(InputPortInfo inputPort)
	{

	inputPortVector.add( inputPort );

	}
	public  void addOutputPort(OutputPortInfo outputPort)
	{

	outPortVector.add(outputPort);

	}
	public  void addInterface(InterfaceDefinition interfaceDefinition)

	{
	interfaceDefintionVector.add( interfaceDefinition );


	}

	public  void addType(TypeDefinition typeDefinition)

	{
       if(!(typeDefintionLinkNameVector.contains( typeDefinition.id())))
	  {
	    if(!(typeDefintionNameVector.contains( typeDefinition.id())))
		{

				typeDefinitonVector.add( typeDefinition );
				typeDefintionNameVector.add(typeDefinition.id());

		}

	  }
	

	}
	public  void addLinkedType(TypeDefinitionLink typeDefinitionLink)

	{
      if(!(typeDefintionLinkNameVector.contains( typeDefinitionLink.id())))
	  {
	    if(!(typeDefintionNameVector.contains( typeDefinitionLink.id())))
		{

				typeDefinitionLinkVector.add( typeDefinitionLink );
				typeDefintionNameVector.add(typeDefinitionLink.id());

		}

	  }
		

	}
	public void write() throws IOException{

	for (InputPortInfo inputPort:inputPortVector){
	String location = inputPort.location() == null ? "" : inputPort.location().toString();
		writer.write( "<H3>" + "Documentation for Port " + inputPort.id() + "</H3>" );

		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>Port Name</th>" );
		writer.write( "<th>Location</th>" );
		writer.write( "<th>Protocol ID</th>" );
		//writer.write( "<th>Code</th>" );
		writer.write( "</tr>" );
		writer.write( "<tr>" );
		writer.write( "<td>" + inputPort.id() + "</td>" );
		writer.write( "<td>" + location + "</td>" );
		writer.write( "<td>" + inputPort.protocolId() + "</td>" );
		//writer.write( "<td>" + "<a href=\"#Code\"> CodePort </a><br />" + "</td>" + "<BR>" );
		writer.write( "</tr>" );

		if ( inputPort.getDocumentation() != null ) {
			writer.write( inputPort.getDocumentation() );
		}
		List<InterfaceDefinition> interfacesList = inputPort.getInterfaceList();
		writer.write( "<tr>" );
		writer.write( "</tr>" );
		writer.write( "<table border=\"1\">" );
		writer.write( "<tr>" );
		writer.write( "<th>InterfaceName</th>" );
		writer.write( "</tr>" );

		for( InterfaceDefinition interfaceDefintion : interfacesList ) {

			String interfaceSourceName = interfaceDefintion.context().sourceName().substring( interfaceDefintion.context().sourceName().lastIndexOf( "/" ) + 1 );
			String interfaceHtmlFileName=interfaceSourceName.replace( ".", "_")+".html";

				writer.write( "<tr>" );
				writer.write( "<td>" + "<A href=\"" + interfaceDefintion.name() + "\">" + interfaceDefintion.name() + " </A></td>" );
				writer.write( "</tr>" + "<BR>" );



		}

      writer.write( "</table>" );

	}

	for (InterfaceDefinition interfaceDefinition:interfaceDefintionVector)
	{
	   writer.write( "<H3>" + "Interface definition " + interfaceDefinition.name() + "</H3>" );
		writer.write( "<a name=\"" + interfaceDefinition.name() + "\"></a>" );
		if ( !(interfaceDefinition.getDocumentation() == null) ) {
			writer.write("<BR>");
			writer.write( interfaceDefinition.getDocumentation() );
			writer.write("<BR>");
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
				if (((RequestResponseOperationDeclaration) operation).requestType().hasSubTypes()){



					writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );


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


					writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );


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
		
	for (TypeDefinition typesDefinition :typeDefinitonVector){
         writer.write( "<H3 id=\""+ typesDefinition.id() +"\">"+"Type " + typesDefinition.id() + "</H3>" );
			writer.write( "<a name=\"" + typesDefinition.id() + "\"></a>" );
		
			writeType( typesDefinition, false, writer, 0 );
			if (typesDefinition.hasSubTypes()){
			writer.write( "<BR>}" );
		}
       }

	for (TypeDefinitionLink typesDefinitionLink :typeDefinitionLinkVector){
         writer.write( "<H3 id=\""+ typesDefinitionLink.linkedTypeName() +"\">"+"Type " + typesDefinitionLink.linkedTypeName() + "</H3>" );
			writer.write( "<a name=\"" + typesDefinitionLink.linkedTypeName() + "\"></a>" );
			writer.write( "\n" );
			writeType( typesDefinitionLink.linkedType(), false, writer, 0 );
			if (typesDefinitionLink.hasSubTypes()){
			writer.write( "<BR>}" );
		    }
       }
	writer.flush();
	writer.close();
	}
	private void writeType( TypeDefinition type, boolean subType, Writer writer, int indetationLevel )
		throws IOException
	{
		StringBuilder builder = new StringBuilder();
		System.out.print( "the Type is : "+type.id()+"\n" );
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
			System.out.print( link.linkedType().id()+"\n" );

			String supportLinkedTypeFileName = link.linkedType().context().source().getSchemeSpecificPart().substring(link.linkedType().context().source().getSchemeSpecificPart().lastIndexOf( "/")+1 );
			builder.append( "<a href=\"#" + link.linkedTypeName() + "\">" + link.linkedTypeName() + "</a>" );
			if (!(link.linkedType().context().sourceName().equals( type.context().sourceName()))){


			}
			
			if ( subType == false ) {
//
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
