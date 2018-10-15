/**
 * *************************************************************************
 * Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com> *
 * Copyright (C) 2011 by Claudio Guidi <cguidi@italianasoftware.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 **************************************************************************
 */
package jolie.doc.impl.html;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;

/**
 *
 * @author balint maschio, claudio guidi
 */
public class JolieDocWriter
{
	private PortInfo port;
	private final List<String> typeDefintionNameVector;
	private final List<TypeDefinition> typeDefinitionVector;
	private final List<TypeDefinitionLink> typeDefinitionLinkVector;
	private final List<String> typeDefintionLinkNameVector;
	private final List<InterfaceDefinition> interfaceDefintionVector;
	private final Writer writer;
	private final String originalFilename;

	public JolieDocWriter( Writer writer, String originalFilename )
	{
		this.writer = writer;
		typeDefinitionLinkVector = new ArrayList<TypeDefinitionLink>();
		typeDefintionLinkNameVector = new ArrayList<String>();
		typeDefinitionVector = new ArrayList<TypeDefinition>();
		typeDefintionNameVector = new ArrayList<String>();
		interfaceDefintionVector = new ArrayList<InterfaceDefinition>();
		this.originalFilename = originalFilename;
	}

	public void addPort( PortInfo port )
	{
		this.port = port;
	}

	public void addInterface( InterfaceDefinition interfaceDefinition )
	{
		interfaceDefintionVector.add( interfaceDefinition );
	}

	public void addType( TypeDefinition typeDefinition )
	{
		if ( (!NativeType.isNativeTypeKeyword( typeDefinition.id() )) && (!typeDefinition.id().equals( "undefined" )) ) {
			if ( !(typeDefintionLinkNameVector.contains( typeDefinition.id() )) && !(typeDefintionNameVector.contains( typeDefinition.id() )) ) {
				typeDefinitionVector.add( typeDefinition );
				typeDefintionNameVector.add( typeDefinition.id() );
			}
		}
	}

	public void addLinkedType( TypeDefinitionLink typeDefinitionLink )
	{
		if ( !(typeDefintionNameVector.contains( typeDefinitionLink.linkedTypeName() )) ) {
			typeDefinitionLinkVector.add( typeDefinitionLink );
			typeDefintionNameVector.add( typeDefinitionLink.linkedTypeName() );
		}
	}

	private void writeHead()
		throws IOException
	{
		writer.write( "<head><style>"
			+ "body { font-size:14px; font-family:Courier; }"
			+ "a { color:#000099;}"
			+ "table {"
			+ "font-size:14px; font-family:Sans-serif;"
			+ "border-collapse:collapse;"
			+ "text-align:left;"
			+ "}"
			+ "table, th, td {"
			+ "border:1px solid #AAAAAA;"
			+ "padding:7px;"
			+ "}"
			+ "li {font-family:Sans-serif;}"
			+ " h1 { font-size:26px; font-family:Sans-serif;}"
			+ " h2 { font-size:20px; font-family:Sans-serif;}"
			+ " h3 { font-size:16px;font-family:Sans-serif; color:#003300; }"
			+ " th { font-size:16px;font-family:Sans-serif; color:black; }"
			+ ".native { font-weight:bold; color:#990000; }"
			+ ".opdoc { font-family: Sans-serif; }"
			+ ".operation-title { background-color: #EEEEEE; width:600px; height:20px; }"
			+ ".code{ white-space:pre; }"
			+ "</style></head>" );
	}

	public void write()
		throws IOException
	{

		// document init
		writer.write( "<html>" );

		writeHead();

		writer.write( "<body>" );

		// port
		String location;
		String protocol;
		if ( port instanceof OutputPortInfo ) {
			OutputPortInfo htmlPort = (OutputPortInfo) port;
			location = htmlPort.location() == null ? "" : htmlPort.location().toString();
			protocol = htmlPort.protocolId() == null ? "" : htmlPort.protocolId();
		} else {
			InputPortInfo htmlPort = (InputPortInfo) port;
			location = htmlPort.location() == null ? "" : htmlPort.location().toString();
			protocol = htmlPort.protocolId();
		}

		writer.write( "<h1>" + "JolieDoc for Port " + port.id() + "</h1>" );
		writer.write( "<h2>From file <code>" + originalFilename + "</code></h2>" );
		if ( !(port.getDocumentation() == null) ) {
			writer.write( port.getDocumentation().trim().replace( "\n", "<br/>" ) );
			writer.write( "<br/><br/>" );
		}
		writer.write( "<table>" );
		writer.write( "<tr>" );
		writer.write( "<th>Port Name</th>" );
		writer.write( "<th>Location</th>" );
		writer.write( "<th>Protocol</th>" );
		//JolieDocWriter.write( "<th>Code</th>" );
		writer.write( "</tr>" );
		writer.write( "<tr>" );
		writer.write( "<td>" + port.id() + "</td>" );
		writer.write( "<td>" + location + "</td>" );
		writer.write( "<td>" + protocol + "</td>" );
		//JolieDocWriter.write( "<td>" + "<a href=\"#Code\"> CodePort </a><br />" + "</td>" + "<BR>" );
		writer.write( "</tr>" );

		writer.write( "</table>" );

		// generating interface list
		//writer.write( "<br>" );
		writer.write( "<h2>" + "List of the available interfaces</h2>" );
		writer.write( "<ul>" );

		for( InterfaceDefinition interfaceDefinition : interfaceDefintionVector ) {
			writer.write( "<li><a href=\"#" + interfaceDefinition.name() + "\">" + interfaceDefinition.name() + " </a>" );
		}
		writer.write( "</ul>" );
		writer.write( "<hr>" );

		// interface tables
		for( InterfaceDefinition interfaceDefinition : interfaceDefintionVector ) {
			writer.write( "<h2 id=" + interfaceDefinition.name() + ">" + "Interface " + interfaceDefinition.name() + "</h2>" );
			writer.write( "<a name=\"" + interfaceDefinition.name() + "\"></a>" );
			if ( !(interfaceDefinition.getDocumentation() == null) ) {
				writer.write( interfaceDefinition.getDocumentation().trim().replace( "\n", "<br>" ) );
				//writer.write( "<BR><BR>" );
			}

			OperationDeclaration operation;
			writer.write( "<table border=\"1\">" );
			writer.write( "<tr>" );
			writer.write( "<th>Heading</th>" );
			writer.write( "<th>Input type</th>" );
			writer.write( "<th>Output type</th>" );
			writer.write( "<th>Faults</th>" );
			writer.write( "</tr>" );

			// scanning operations into the interface
			ArrayList<String> keylist = new ArrayList<String>();
			for( String key : interfaceDefinition.operationsMap().keySet() ) {
				keylist.add( key );
			}
			Collections.sort( keylist );
			Iterator keyiterator = keylist.iterator();
			while( keyiterator.hasNext() ) {
				String key = (String) keyiterator.next();

				operation = interfaceDefinition.operationsMap().get( key );
				writer.write( "<tr>" );
				writer.write( "<td><a href=\"#" + operation.id() + "\">" + operation.id() + "</a></td>" );
				if ( operation instanceof RequestResponseOperationDeclaration ) {
                    if (((RequestResponseOperationDeclaration) operation).requestType() instanceof TypeInlineDefinition &&
                            ((TypeInlineDefinition) ((RequestResponseOperationDeclaration) operation).requestType()).hasSubTypes()) {
						writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "</a><br />" + "</td>" );
					} else {
						writer.write( "<td>" + ((RequestResponseOperationDeclaration) operation).requestType().id() + "<br />" + "</td>" );
					}

                    if (((RequestResponseOperationDeclaration) operation).responseType() instanceof TypeInlineDefinition &&
                            ((TypeInlineDefinition) ((RequestResponseOperationDeclaration) operation).responseType()).hasSubTypes()) {
						writer.write( "<td>" + "<a href=\"#" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "\">" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "</a><br />" + "</td>" );
					} else {
						writer.write( "<td>" + ((RequestResponseOperationDeclaration) operation).responseType().id() + "<br />" + "</td>" );
					}

					writer.write( "<td>" );
					for( Entry<String, TypeDefinition> fault : ((RequestResponseOperationDeclaration) operation).faults().entrySet() ) {
						if ( !fault.getValue().id().equals( "undefined" ) ) {
							writer.write( fault.getKey() + "( <a href=\"#" + fault.getValue().id() + "\">" + fault.getValue().id() + "</a> )&nbsp;&nbsp;<br>" );
						} else {
							writer.write( fault.getKey() + ",&nbsp;<br>" );
						}
					}
					writer.write( "</td>" );
					writer.write( "</tr>" );
				}
				if ( operation instanceof OneWayOperationDeclaration ) {
					writer.write( "<td>" + "<a href=\"#" + ((OneWayOperationDeclaration) operation).requestType().id()
						+ "\">" + ((OneWayOperationDeclaration) operation).requestType().id() + "</a><br /></td><td>&nbsp;</td><td>&nbsp;</td>" );
					writer.write( "</tr>" );
					writer.write( "</tr>" );
				}
			}
			writer.write( "</table>" );
		}
		writer.write( "<hr>" );

		// Operation details
		writer.write( "<h2>Operation list</h2>" );
		for( InterfaceDefinition interfaceDefinition : interfaceDefintionVector ) {
			for( Entry<String, OperationDeclaration> entry : interfaceDefinition.operationsMap().entrySet() ) {
				OperationDeclaration operation = entry.getValue();
				writer.write( "<div class=\"operation-title\"><a name=\"" + operation.id() + "\"></a><h3 id=\"" + operation.id() + "\">" + operation.id() + "</h3></div>" );
				if ( operation instanceof RequestResponseOperationDeclaration ) {
					RequestResponseOperationDeclaration rrOperation = (RequestResponseOperationDeclaration) operation;
					writer.write( "<pre>" + operation.id() + "( <a href=\"#" + rrOperation.requestType().id() + "\">" + rrOperation.requestType().id()
						+ "</a> )( <a href=\"#" + rrOperation.responseType().id() + "\">" + rrOperation.responseType().id() + "</a> )" );
					boolean faultExist = false;
					for( Entry<String, TypeDefinition> fault : ((RequestResponseOperationDeclaration) operation).faults().entrySet() ) {
						if ( !faultExist ) {
							writer.write( " throws" );
							faultExist = true;
						}
						writer.write( "\n" + indent( 4 ) );
						if ( !fault.getValue().id().equals( "undefined" ) ) {
							writer.write( fault.getKey() + "( <a href=\"#" + fault.getValue().id() + "\">" + fault.getValue().id() + "</a> )" );
						} else {
							writer.write( fault.getKey() );
						}
					}
					writer.write( "</pre>" );
				} else {
					OneWayOperationDeclaration owOperation = (OneWayOperationDeclaration) operation;
					writer.write( "<p><pre>" + operation.id() + "( <a href=\"#" + owOperation.requestType().id() + "\">" + owOperation.requestType().id() + "</a> )</pre></p>" );
				}
				if ( operation.getDocumentation() != null ) {
					writer.write( "<span class=\"opdoc\"><p>" + operation.getDocumentation().trim().replace( "\n", "<br>" ) + "</p></span>" );
				}

			}
		}

		writer.write( "<hr>" );
		writer.write( "<h2>Message type list</h2>" );

		// scanning type list
		for( TypeDefinition typesDefinition : typeDefinitionVector ) {
			writer.write( "<a name=\"" + typesDefinition.id() + "\"></a><h3 id=\"" + typesDefinition.id() + "\">" + typesDefinition.id() + "</h3>" );
			if ( typesDefinition.getDocumentation() != null ) {
				writer.write( "<span class=\"opdoc\"><p>" + typesDefinition.getDocumentation().trim().replace( "\n", "<br/>" ) + "</p></span>" );
			}
			writer.write( "<div class=\"code\" lang=\"jolie\">" + writeType( typesDefinition, false, false, 0 ) + "</div>" );
		}

		writer.write( "<hr>" );
		writer.write( "<h2>Type list</h2>" );
		for( TypeDefinitionLink typesDefinitionLink : typeDefinitionLinkVector ) {
			writer.write( "<h3 id=\"" + typesDefinitionLink.linkedTypeName() + "\">" + typesDefinitionLink.linkedTypeName() + "</h3>" );
			writer.write( "<a name=\"" + typesDefinitionLink.linkedTypeName() + "\"></a>" );
			writer.write( "<div class=\"code\" lang=\"jolie\">" + writeType( typesDefinitionLink.linkedType(), false, false, 0 ) + "</div>" );
		}

		// document ending
		writer.write( "</body>" );
		writer.write( "</html>" );

		// writing and closing
		writer.flush();
		writer.close();
	}

	private String writeType( TypeDefinition type, boolean subType, boolean choice, int indetationLevel )
		throws IOException
	{
		StringBuilder builder = new StringBuilder();
		if ( subType ) {
			for( int indexIndetation = 0; indexIndetation < indetationLevel; indexIndetation++ ) {
				builder.append( " " );
			}
			builder.append( "." + type.id() + getCardinalityString( type ) + ": ");
        } else if (!choice){
            builder.append("type " + type.id() + ": ");
        }

		if ( type instanceof TypeDefinitionLink ) {
			TypeDefinitionLink link = (TypeDefinitionLink) type;
			builder.append( /* "<a href=\"#" + */ link.linkedTypeName() /* + "\">" + link.linkedTypeName() + "</a>" */ );

		} else if (type instanceof TypeInlineDefinition) {
            if (((TypeInlineDefinition) type).untypedSubTypes()) {
                builder.append("undefined");
            } else {
                builder.append( /*"<span class=\"native\">" + */ nativeTypeToString(((TypeInlineDefinition) type).nativeType()) /* + "</span>" */);
                if (((TypeInlineDefinition) type).hasSubTypes()) {
                    builder.append(" { \n");
                    for (Entry<String, TypeDefinition> entry : ((TypeInlineDefinition) type).subTypes()) {
                        builder.append(writeType(entry.getValue(), true, false, indetationLevel + 4) + "\n");
                    }
                    for (int indexIndetation = 0; indexIndetation < indetationLevel; indexIndetation++) {
                        builder.append(" ");
                    }
                    ;
                    builder.append("}");
                }
            }
            
        } else if (type instanceof TypeChoiceDefinition){
            builder.append(writeType(((TypeChoiceDefinition) type).left(), false, true, 0));
            builder.append(" | ");
            builder.append(writeType(((TypeChoiceDefinition) type).right(), false, true, 0) );
        }

		return builder.toString();
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

	private String indent( int n )
	{
		String indentation = "";
		for( int x = 0; x < n; x++ ) {
			indentation = indentation + "\t";
		}
		return indentation;
	}
}
