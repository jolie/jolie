/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jolie.doc.impl.html;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
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
import support.GeneralDocumentCreator;
import support.GeneralProgramVisitor;

/**
 *
 * @author balint
 */
public class HtmlDocumentCreatorNew extends GeneralDocumentCreator {
private Writer writer;
private List<String> filesList;
private HashMap<String, String> HyperlinkMap;
private String nameFile;
 public HtmlDocumentCreatorNew(GeneralProgramVisitor program){
        super (program);
}

    @Override
    public void ConvertDocument() {
            filesList=GetFilesNameList();
            InputPortInfo[] inputPortInfoList = GetInputPortArray();
            OutputPortInfo[] outputPortInfoList = super.GetOutputPortArray();
            InterfaceDefinition[] interfacesList = GetInterfaceArray();
            List<Map<String, TypeDefinition>> typesList = GetTypesSet();
            HyperlinkMap= new HashMap<String, String>();
            System.out.print("the dimension of the list of file is : "+ filesList.size()+ "\n");
            System.out.print("the dimension of the list of outort is : "+ outputPortInfoList.length+ "\n");

            for (String namefile:filesList)
            {
            String supportNameFile = namefile.substring(namefile.lastIndexOf("/") + 1, namefile.length());
            supportNameFile.replace(".", "_");
            supportNameFile+=".html";
            HyperlinkMap.put(namefile, supportNameFile);
            }
        for ( String namefile: filesList)
        {
            try {
                this.nameFile = HyperlinkMap.get(namefile);
                System.out.print("the name of the file created is : " + nameFile + "\n");
                try {
                    writer = new BufferedWriter(new FileWriter(nameFile));
                } catch (IOException ex) {
                    Logger.getLogger(HtmlDocumentCreatorNew.class.getName()).log(Level.SEVERE, null, ex);
                }
                for (InputPortInfo inputPortInfo : inputPortInfoList) {
                    if (inputPortInfo.context().sourceName().equals(namefile)) {
                        try {
                            ConvertInputPorts(inputPortInfo, writer);
                        } catch (IOException ex) {
                            Logger.getLogger(HtmlDocumentCreatorNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                for (OutputPortInfo outputPort : outputPortInfoList) {
                    if (outputPort.context().sourceName().equals(namefile)) {
                        try {
                            ConvertOutputPorts(outputPort, writer);
                        } catch (IOException ex) {
                            Logger.getLogger(HtmlDocumentCreatorNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                for (InterfaceDefinition interfaceDefintion : interfacesList) {
                    if (interfaceDefintion.context().sourceName().equals(namefile)) {
                        try {
                            ConvertInterface(interfaceDefintion, writer);
                        } catch (IOException ex) {
                            Logger.getLogger(HtmlDocumentCreatorNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                for (Map<String, TypeDefinition> typeDefinitionMap : typesList) {
                    if (namefile.equals(typeDefinitionMap.entrySet().iterator().next().getKey())) {
                        try {
                            ConvertTypes(typeDefinitionMap.entrySet().iterator().next().getValue(), writer);
                        } catch (IOException ex) {
                            Logger.getLogger(HtmlDocumentCreatorNew.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(HtmlDocumentCreatorNew.class.getName()).log(Level.SEVERE, null, ex);
            }

        }


    }

    @Override
    public void ConvertInterface(InterfaceDefinition interfaceDefinition, Writer writer) throws IOException {
        writer.write( "<H3>" + "Interface definition " + interfaceDefinition.name() + "</H3>" );
				// writer.write(interfaceDefinition.getDocument());
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
						
						String supportFileName = (((RequestResponseOperationDeclaration) operation).requestType().context().sourceName());
						String supportHyperLinkFileName = (supportFileName.substring( supportFileName.lastIndexOf( "/" ) + 1, supportFileName.length() ).replace( ".", "_" ));
						supportHyperLinkFileName += ".html";
						if ( !((RequestResponseOperationDeclaration) operation).requestType().context().sourceName().equals( this.nameFile ) ) {
							writer.write( "<td>" + "<a href=\"" + supportHyperLinkFileName + "\"" + "target=\"" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
						} else {

							writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );

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

						
						writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
						writer.write( "</tr>" );
					}
				}
				writer.write( "</table>" );
    }

    @Override
    public void ConvertOutputPorts(OutputPortInfo outputPortInfo, Writer writer) throws IOException {
     
				String location = outputPortInfo.location() == null ? "" : outputPortInfo.location().toString();
				writer.write( "<H3>"+ "Documentation for Port " + outputPortInfo.id() + "</H3>" );

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
				for( int counterInterfaces = 0; counterInterfaces < interfacesList.size() ; counterInterfaces++ ) {



					writer.write( "<tr>" );
					writer.write( "<td>" + "<A href=\"" + HyperlinkMap.get( interfacesList.get( counterInterfaces ) ) + "\">" + interfacesList.get( counterInterfaces ) + " </A></td>" );
					writer.write( "</tr>" + "<BR>" );


				}
			
    }

    @Override
    public void ConvertInputPorts(InputPortInfo inputPortInfo, Writer writer) throws IOException{


				writer.write( "<H3>"+ "Documentation for Port " + inputPortInfo.id() + "</H3>");

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
					writer.write( "<td>" + "<A href=\"" + HyperlinkMap.get( interfacesListLocal.get(counterInterfaces) ) + "\">" + interfacesListLocal.get( counterInterfaces ) + " </A></td>" );
					writer.write( "</tr>" + "<BR>");
				}

    }

    @Override
    public void ConvertOperations(OperationDeclaration operationDeclaration, Writer writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void ConvertTypes(TypeDefinition typesDefinition, Writer writer) throws IOException {
    

			if ( typesDefinition.hasSubTypes() ) {

				writer.write( "<H3>" + "Type " + typesDefinition.id() +"</H3>" );
				writer.write( "<a name=\"" + typesDefinition.id() + "\"></a>" );
				writer.write( "\n" );
				writeType( typesDefinition, false, writer );
			}
		
		
    }

    private void writeType(TypeDefinition type, boolean subType, Writer writer) throws IOException {
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
