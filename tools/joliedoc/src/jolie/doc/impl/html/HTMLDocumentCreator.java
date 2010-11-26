/***************************************************************************
 *   Copyright (C) 2010 by Balint Maschio <bmaschio@italianasoftware.com>  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.doc.impl.html;

import com.sun.jmx.remote.util.OrderClassLoaders;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.doc.DocumentCreationException;
import jolie.doc.DocumentCreator;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.Program;
import jolie.doc.ProgramVisitor;
import javax.swing.text.html.HTMLEditorKit;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import sun.jdbc.odbc.OdbcDef;

public class HTMLDocumentCreator implements DocumentCreator
{
	private final String NEWLINE = "\n";
	private final String OPENH1HEADER = "<h1>";
	private final String CLOSEH1HEADER = "</h1>";
	private final String OPENH2HEADER = "<h2>";
	private final String CLOSEH2HEADER = "</h2>";
	private final String OPENH3HEADER = "<h3>";
	private final String CLOSEH3HEADER = "</h3>";
	private final String OPENLIST = "<ol>";
	private final String CLOSELIST = "</ol>";
	private final String OPENLISTELEMENT = "<li>";
	private final String CLOSELISTELEMENT = "</li>";
	private final String QUOTATIONMARK = Character.toString( '"' );
	private File outputDirectory;
	private HTMLEditorKit.HTMLFactory htmlObj;
	private Set<RequestResponseOperationDeclaration> requestResponce;
	private Set<OneWayOperationDeclaration> oneWay;
	private String nameFile;
	private List< String > nameFiles;
	private HashMap<String, String> HyperlinkMap;
        private Set<TypeDefinition> types;

	public HTMLDocumentCreator()
	{
		outputDirectory = new File( "" );
		requestResponce = new HashSet<RequestResponseOperationDeclaration>();
		oneWay = new HashSet<OneWayOperationDeclaration>();
		HyperlinkMap = new HashMap<String, String>();
                types= new LinkedHashSet<TypeDefinition>();
	}

	public void createDocument( Program program, String nameFile )
		throws DocumentCreationException
	{
		this.nameFile = nameFile;
		try {
			ProgramVisitor programVisitor = new ProgramVisitor( program );
			programVisitor.run();
			InterfaceDefinition[] interfaceDefinitions =
				programVisitor.getInterfaceDefinitions();

			nameFiles = new ArrayList< String >();
			for( InterfaceDefinition i : interfaceDefinitions ) {

				HyperlinkMap.put( i.name(), i.context().sourceName().substring( i.context().sourceName().lastIndexOf( "/" ) + 1, i.context().sourceName().length() ).replace( ".", "_" ) + ".html" );
				if ( !(nameFiles.contains( i.context().sourceName() )) ) {
					nameFiles.add( i.context().sourceName() );
				}
			}

			InputPortInfo[] inputPortDefinitions =
				programVisitor.getInputPortInfo();

			for( InputPortInfo i : inputPortDefinitions ) {
				// i.context().sourceName().
				if ( !(nameFiles.contains( i.context().sourceName() )) ) {
					nameFiles.add( i.context().sourceName() );
				}
			}
			OutputPortInfo[] outputPortDefinitions =
				programVisitor.getOutputPortInfo();

			for( OutputPortInfo i : outputPortDefinitions ) {
				// i.context().sourceName().
				if ( !(nameFiles.contains( i.context().sourceName() )) ) {
					nameFiles.add( i.context().sourceName() );
				}
			}
			for( OneWayOperationDeclaration oneWayObject : oneWay ) {
				if ( !(nameFiles.contains( oneWayObject.context().sourceName() )) ) {
					nameFiles.add( oneWayObject.context().sourceName() );
				}

			}
			for( RequestResponseOperationDeclaration requestResponceObject : requestResponce ) {
				if ( !(nameFiles.contains( requestResponceObject.context().sourceName() )) ) {
					nameFiles.add( requestResponceObject.context().sourceName() );
				}
			}
			//nameFile.substring(0,nameFile.length()-3);
			for( int CounterFiles = 0; CounterFiles < nameFiles.size(); CounterFiles++ ) {
				System.out.print( nameFiles.get( CounterFiles ).substring( nameFiles.get( CounterFiles ).lastIndexOf( "/" ) + 1, nameFiles.get( CounterFiles ).length() ) );
				String nameFileSupport = nameFiles.get( CounterFiles ).substring( nameFiles.get( CounterFiles ).lastIndexOf( "/" ) + 1, nameFiles.get( CounterFiles ).length() ).replace( ".", "_" );
				this.nameFile = nameFiles.get( CounterFiles );
				Writer writer = new BufferedWriter( new FileWriter( nameFileSupport + ".html" ) );

				writer.write( OPENH1HEADER + "DOCUMENTATION FOR "
					+ this.nameFile.substring( this.nameFile.lastIndexOf( "/" ) + 1, this.nameFile.length() )
					+ CLOSEH1HEADER );



				writer.write( "<ul id=" + QUOTATIONMARK + "toc" + QUOTATIONMARK + ">" );
				writer.write( OPENLISTELEMENT + "<span>Interfaces</span><a href=\"#interface\"> Link</a><br />" + CLOSELISTELEMENT );
				writer.write( OPENLISTELEMENT + "<span>Output Ports</span><a href=" + QUOTATIONMARK + "#uports" + QUOTATIONMARK
					+ "> Link</a><br />" + CLOSELISTELEMENT );
				writer.write( OPENLISTELEMENT + "<span>Input Ports </span><a href=" + QUOTATIONMARK + "#iports" + QUOTATIONMARK
					+ "> Link</a><br />" + CLOSELISTELEMENT );



				writer.write( "</ul>" );


				//	programVisitor.getInterfaceDefinitions();
				boolean interfacesExist = false;
				for( InterfaceDefinition i : interfaceDefinitions ) {
					if ( i.context().sourceName().equals( this.nameFile ) ) {
						interfacesExist = true;

					}



				}
				if ( interfacesExist ) {
					writer.write( "<H2 id=" + QUOTATIONMARK + "interface" + QUOTATIONMARK + ">Interfaces Definitions</H2>" );

					for( InterfaceDefinition i : interfaceDefinitions ) {
						// i.context().sourceName().

						convertInterfaceDefinition( i, writer );

					}
				}
				boolean outportExist = false;
				for( OutputPortInfo i : outputPortDefinitions ) {

					if ( i.context().sourceName().equals( this.nameFile ) ) {
						outportExist = true;

					}
				}
				if ( outportExist ) {
					writer.write( "<H2 id=" + QUOTATIONMARK + "uports" + QUOTATIONMARK + ">Output Ports Definitions</H2>" );
					for( OutputPortInfo i : outputPortDefinitions ) {

						convertOutputPortDefinitions( i, writer );
					}
				}

				boolean inputportExist = false;
				for( InputPortInfo i : inputPortDefinitions ) {

					if ( i.context().sourceName().equals( this.nameFile ) ) {
						inputportExist = true;

					}
				}
				if ( inputportExist ) {
					writer.write( "<H2 id=" + QUOTATIONMARK + "iports" + QUOTATIONMARK + ">Input Ports Definitions</H2>" );
					for( InputPortInfo i : inputPortDefinitions ) {
						convertInputPortDefinitions( i, writer );
					}
				}
				for( OneWayOperationDeclaration oneWayObject : oneWay ) {
					convertTypeDefintion( oneWayObject.requestType(), writer );

				}
				for( RequestResponseOperationDeclaration requestResponceObject : requestResponce ) {
					convertTypeDefintion( requestResponceObject.requestType(), writer );
				}
				writer.flush();
				writer.close();
			}
		} catch( IOException e ) {

			throw (new DocumentCreationException( e ));
		}


	}

	private void convertInterfaceDefinition( InterfaceDefinition node, Writer writer )
		throws DocumentCreationException
	{
		try {

			if ( node.context().sourceName().equals( nameFile ) ) {
				writer.write( OPENH3HEADER + "Interface definition " + node.name() + CLOSEH3HEADER );
				// writer.write(node.getDocument());
				OperationDeclaration operation;
				writer.write( "<table border=\"1\">" );
				writer.write( "<tr>" );
				writer.write( "<th>Heading</th>" );
				writer.write( "<th>Input type</th>" );
				writer.write( "<th>Output type</th>" );
				writer.write( "</tr>" );
				for( Entry<String, OperationDeclaration> entry :
					node.operationsMap().entrySet() ) {
					if ( node.getDocumentation() != null ) {
						writer.write( node.getDocumentation() );
					}
					operation = entry.getValue(); // questo è l'oggetto operationdeclaration

					writer.write( "<tr>" );
					writer.write( "<td>" + operation.id() + "</td>" );
					//writer.write( "<td>" + "<a href=\"#Code\"> Code </a><br />" + "</td>" + NEWLINE );

					if ( operation instanceof RequestResponseOperationDeclaration ) {
						requestResponce.add( (RequestResponseOperationDeclaration) operation );
						String supportFileName = (((RequestResponseOperationDeclaration) operation).requestType().context().sourceName());
						String supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
						supportHyperLinkFileName += ".html";
						if ( !((RequestResponseOperationDeclaration) operation).requestType().context().sourceName().equals( this.nameFile ) ) {
							writer.write( "<td>" + "<a href=\"" + supportHyperLinkFileName + "\"" + "target=\"" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
						} else {

							writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );

						}

						populateTypesSet(((RequestResponseOperationDeclaration)operation).requestType());
						Integer index=0;
                                                 for (Iterator i = types.iterator();i.hasNext();)
                                                  {
                                                    TypeDefinition support= (TypeDefinition)i.next();
                                                     System.out.print(index.toString()+" "+support.id()+"\n");
                                                     index++;
                                                  }

                                                supportFileName = (((RequestResponseOperationDeclaration) operation).responseType().context().sourceName());
						supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
						System.out.print( ((RequestResponseOperationDeclaration) operation).responseType().id() + "  il file e :" + supportHyperLinkFileName + "\n" );
						//System.in.read();
						supportHyperLinkFileName += ".html";
						if ( !((RequestResponseOperationDeclaration) operation).responseType().context().sourceName().equals( this.nameFile ) ) {
							writer.write( "<td>" + "<a href=\"" + supportHyperLinkFileName + "\"" + "target=\"" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );
						} else {

							writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );

						}
						//writer.write( "<td>"+"<a href=\""+supportHyperLinkFileName+"\""+"target=\""+((RequestResponseOperationDeclaration) operation).responseType().id()+"\">"+((RequestResponseOperationDeclaration) operation).responseType().id()+"</a><br />"+ "</td>");
						writer.write( "</tr>" );
					}
					if ( operation instanceof OneWayOperationDeclaration ) {

						oneWay.add( (OneWayOperationDeclaration) operation );
						writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
						writer.write( "</tr>" );
					}
				}
				writer.write( "</table>" );

			}
		} catch( IOException e ) {

			throw (new DocumentCreationException( e ));
		}


	}
        private  void populateTypesSet(TypeDefinition typeDefinition){

          if (!(types.contains(typeDefinition))){
            System.out.print("\n"+"add type "+ typeDefinition.id()+"\n");
            types.add(typeDefinition);
                System.out.print("this is the native type "+typeDefinition.nativeType().id()+"\n");
                System.out.print("has subtypes "+typeDefinition.hasSubTypes()+"\n");
               // System.out.print("has Linked type "+typeDefinition.+"\n");
                if (typeDefinition.hasSubTypes()){
                Set <Map.Entry<String,TypeDefinition>> supportSet= typeDefinition.subTypes();
                
                for (Iterator i = supportSet.iterator();i.hasNext();)
                {
                   Map.Entry me=(Map.Entry)i.next();

                   System.out.print("element of the list "+me.getKey()+"\n");
                   populateTypesSet((TypeDefinition)me.getValue());
                
                }
              }

          }
          
         
        }
	private void convertOutputPortDefinitions( OutputPortInfo node, Writer writer )
		throws DocumentCreationException
	{

		try {
			if ( node.context().sourceName().equals( nameFile ) ) {
				String location = node.location() == null ? "" : node.location().toString();
				writer.write( OPENH3HEADER + "Documentation for Port " + node.id() + CLOSEH3HEADER );
				writer.write( "<table border=\"1\">" );
				writer.write( "<tr>" );
				writer.write( "<th>Port Name</th>" );
				writer.write( "<th>Location</th>" );
				writer.write( "<th>Protocol ID</th>" );
				writer.write( "<th>Code</th>" );
				writer.write( "</tr>" );
				writer.write( "<tr>" );
				writer.write( "<td>" + node.id() + "</td>" );
				writer.write( "<td>" + location + "</td>" );
				writer.write( "<td>" + node.protocolId() + "</td>" );
				writer.write( "<td>" + "<a href=\"#Code\"> CodePort </a><br />" + "</td>" + NEWLINE );
				writer.write( "</tr>" );

				if ( node.getDocumentation() != null ) {
					writer.write( node.getDocumentation() + NEWLINE );
				}

				List< String > interfacesList = node.getInterfacesList();
				writer.write( "<tr>" );
				writer.write( "</tr>" );
				writer.write( "<table border=\"1\">" );
				writer.write( "<tr>" );
				writer.write( "<th>InterfaceName</th>" );
				writer.write( "</tr>" );
				for( int counterInterfaces = 0; counterInterfaces < node.getInterfacesList().size(); counterInterfaces++ ) {



					writer.write( "<tr>" );
					writer.write( "<td>" + "<A href=\"" + HyperlinkMap.get( interfacesList.get( counterInterfaces ) ) + "\">" + interfacesList.get( counterInterfaces ) + " </A></td>" );
					writer.write( "</tr>" );
					//writer.write("il nome della interfaccia e "+node.getInterfacesList().get(counterInterfaces)+ NEWLINE);

				}

			}
		} catch( IOException e ) {
			throw (new DocumentCreationException( e ));
		}
	}

	private void convertInputPortDefinitions( InputPortInfo node, Writer writer )
		throws DocumentCreationException
	{
		try {
			if ( node.context().sourceName().equals( nameFile ) ) {
				String location = node.location() == null ? "" : node.location().toString();
				writer.write( OPENH3HEADER + "Documentation for Port " + node.id() + CLOSEH3HEADER );

				writer.write( "<table border=\"1\">" );
				writer.write( "<tr>" );
				writer.write( "<th>Port Name</th>" );
				writer.write( "<th>Location</th>" );
				writer.write( "<th>Protocol ID</th>" );
				writer.write( "<th>Code</th>" );
				writer.write( "</tr>" );
				writer.write( "<tr>" );
				writer.write( "<td>" + node.id() + "</td>" );
				writer.write( "<td>" + location + "</td>" );
				writer.write( "<td>" + node.protocolId() + "</td>" );
				writer.write( "<td>" + "<a href=\"#Code\"> CodePort </a><br />" + "</td>" + NEWLINE );
				writer.write( "</tr>" );

				if ( node.getDocumentation() != null ) {
					writer.write( node.getDocumentation() );
				}
				List<String> interfacesList = node.getInterfacesList();
				writer.write( "<tr>" );
				writer.write( "</tr>" );
				writer.write( "<table border=\"1\">" );
				writer.write( "<tr>" );
				writer.write( "<th>InterfaceName</th>" );
				writer.write( "</tr>" );
				for( int counterInterfaces = 0; counterInterfaces < node.getInterfacesList().size(); counterInterfaces++ ) {



					writer.write( "<tr>" );
					writer.write( "<td>" + "<A href=\"" + HyperlinkMap.get( interfacesList.get( counterInterfaces ) ) + "\">" + interfacesList.get( counterInterfaces ) + " </A></td>" );
					writer.write( "</tr>" + NEWLINE );


				}
			}

		} catch( IOException e ) {
			throw (new DocumentCreationException( e ));
		}
	}

	private void convertTypeDefintion( TypeDefinition typeDefintion, Writer writer )
	{


		try {

			if ( typeDefintion.hasSubTypes() ) {

				writer.write( OPENH3HEADER + "Type " + typeDefintion.id() + CLOSEH3HEADER );
				writer.write( "<a name=\"" + typeDefintion.id() + "\"></a>" );
				writer.write( "\n" );
				writeType( typeDefintion, false, writer );
			}
		} catch( IOException e ) {
		}

	}

	public void setDocumentDirectory( File directory )
	{
		this.outputDirectory = directory;



	}

	private void writeType( TypeDefinition type, boolean subType, Writer writer )
		throws IOException
	{
		StringBuilder builder = new StringBuilder();
		if ( subType ) {
			builder.append( "<BR> ." );
		} else {
			builder.append( "type " );
		}
		builder.append( type.id() ).append( getCardinalityString( type ) ).append( ':' );
		if ( type instanceof TypeDefinitionLink ) {
			TypeDefinitionLink link = (TypeDefinitionLink) type;
			builder.append( link.linkedType().id() );

			if ( subType == false ) {
				builder.append( "<BR>" );
			}
			writer.write( builder.toString() );
			writer.write( "<BR>" );
		} else if ( type.untypedSubTypes() ) {
			builder.append( "undefined" );
			writer.write( builder.toString() );
		} else {
			builder.append( nativeTypeToString( type.nativeType() ) );
			if ( type.hasSubTypes() ) {
				builder.append( "<BR> {" );
				builder.append( "<BR>" );
			}
			writer.write( builder.toString() );
			if ( type.hasSubTypes() ) {
				//indent();
				for( Entry<String, TypeDefinition> entry : type.subTypes() ) {
					writeType( entry.getValue(), true, writer );
				}
				//unindent();
			}

			if ( type.hasSubTypes() ) {
				writer.write( "}" );
			}
			if ( subType == false ) {
				writer.write( "" );
			}
		}
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

	private static String nativeTypeToString( NativeType nativeType )
	{
		return (nativeType == null) ? "" : nativeType.id();
	}
	/**  public void SetFont(InputPortInfo n , Object T);
	public void SetFontSize(InputPortInfo n, int Size);
	public void SetColour (InputPortInfo n, String Colour);
	public void SetFont(CommentDoc n , Object T);
	public void SetFontSize(CommentDoc n, int Size);
	public void SetColour (CommentDoc n, String Colour);
	public void SetFont(OutputPortInfo n , Object T);
	public void SetFontSize(OutputPortInfo n, int Size);
	public void SetColour (OutPortInfo n, String T);
	
	 *
	 *
	 */
}
