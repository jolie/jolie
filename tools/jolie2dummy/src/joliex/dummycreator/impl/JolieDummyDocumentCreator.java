/***************************************************************************
 *   Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>  *
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

package joliex.dummycreator.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.util.ProgramInspector;

/**
 *
 * @author Balint Maschio
 */
public class JolieDummyDocumentCreator
{
	private ProgramInspector inspector;
	private String directorySourceFile;
	private StringBuilder stringBuilder;
	private String nameSourceFile;

	public JolieDummyDocumentCreator( ProgramInspector inspector, File directorySourceFile )
	{

		this.inspector = inspector;
		this.directorySourceFile = directorySourceFile.getParent() + File.separator;
		this.nameSourceFile = directorySourceFile.getName();
		stringBuilder = new StringBuilder();
	}

	public void createDocument()
		throws FileNotFoundException, IOException
	{
		String lineOriginalFile = "include  \"string_utils.iol\"";
		System.out.print( "The directory of the file ol is " + directorySourceFile + "\n" );
		BufferedReader reader = new BufferedReader( new FileReader( directorySourceFile + nameSourceFile ) );

		while( !(lineOriginalFile.contains( "nullProcess" )) ) {

			stringBuilder.append( lineOriginalFile + "\n" );

			lineOriginalFile = reader.readLine();
			if ( lineOriginalFile == null )
				break;
			if ( (lineOriginalFile.contains( "include" )) && (lineOriginalFile.contains( "/" )) ) {

				int indexSlash = lineOriginalFile.indexOf( "/" );
				lineOriginalFile = lineOriginalFile.substring( 0, indexSlash ) + ".." + lineOriginalFile.substring( indexSlash );
			}

		}

		for( InputPortInfo inputPortInfo : inspector.getInputPorts() ) {
			if ( inputPortInfo.context().source().getSchemeSpecificPart().contains( directorySourceFile ) ) {
				System.out.print( "The port is part of the soa " + inputPortInfo.context().source().getSchemeSpecificPart() + "\n" );
				for( OperationDeclaration operationDeclaration : inputPortInfo.operations() ) {

					convertOperation( operationDeclaration, stringBuilder );
					//Input

				}


			} else {


				System.out.print( "The port is not part of the soa " + inputPortInfo.context().source().getSchemeSpecificPart() + "\n" );

			}
		}





		stringBuilder.append( "}" );
		System.out.print( stringBuilder.toString() );
		File pippo = new File( directorySourceFile + "test/" );
		pippo.mkdir();
		BufferedWriter writer = new BufferedWriter( new FileWriter( directorySourceFile + "test/" + nameSourceFile ) );
		writer.append( stringBuilder.toString() );
		writer.flush();
		writer.close();
	}

	private void convertInterfaces( InterfaceDefinition insterfaceDefinition, StringBuilder stringBuilder )
	{
	}

	private void convertOperation( OperationDeclaration operatationDeclaration, StringBuilder stringBuilder )
	{

		if ( operatationDeclaration instanceof RequestResponseOperationDeclaration ) {
			stringBuilder.append( "[" ).append( operatationDeclaration.id() ).append( "(request)(response){\n" );
			stringBuilder.append( "\t" ).append( "valueToPrettyString@StringUtils( request )( s)" ).append( ";\n" );
			stringBuilder.append( "\t" ).append( "println@Console(s)();\n" );

			TypeDefinition typeDefinition = ((RequestResponseOperationDeclaration) operatationDeclaration).responseType();
			convertTypes( typeDefinition, stringBuilder, "dummyResponse");
			stringBuilder.append("response<<dummyResponse\n" );
			stringBuilder.append( "\n}]{nullProcess}\n" );
		}

	}

	private void convertTypes( TypeDefinition typeDefinition, StringBuilder stringBuilder, String nameVariable) {
		if (typeDefinition instanceof TypeChoiceDefinition) {
			TypeChoiceDefinition typeChoiceDefinition = (TypeChoiceDefinition) typeDefinition;
			convertTypes(typeChoiceDefinition.left(), stringBuilder, nameVariable);
			convertTypes(typeChoiceDefinition.right(), stringBuilder, nameVariable);

		} else if (typeDefinition instanceof TypeDefinitionLink) {
			TypeDefinitionLink typeDefinitionLink = (TypeDefinitionLink) typeDefinition;
			convertTypes(typeDefinitionLink.linkedType(), stringBuilder, nameVariable);

		} else if (typeDefinition instanceof TypeInlineDefinition) {
			TypeInlineDefinition typeInlineDefinition = (TypeInlineDefinition) typeDefinition;
			//check subtypes
			if (typeInlineDefinition.hasSubTypes()) {
				for (Entry<String, TypeDefinition> entry : typeInlineDefinition.subTypes()) {
					convertTypes(entry.getValue(), stringBuilder, nameVariable + "." + entry.getKey());
				}
			} else {
				//get cardinality indexes. if maximum cardinality is considerably big (more than 5), print only first 5 entries; or print less otherwise; don't print cardinality, if it equals 1
				int typeCardinality = (typeInlineDefinition.cardinality().max() > 5 ? 5 : typeInlineDefinition.cardinality().max());
				for (int cardinalityIndex = 0; cardinalityIndex < typeCardinality; cardinalityIndex++) {
					String cardinalityString = typeCardinality == 1 ? "" : "[" + String.valueOf(cardinalityIndex) + "]";
					if (!(typeInlineDefinition.nativeType().id().equals("void"))) {
						if (typeInlineDefinition.nativeType().id().equals("int")) {
							stringBuilder.append( "\t" + nameVariable ).append(cardinalityString).append("=").append("42;\n");
						} else if (typeInlineDefinition.nativeType().id().equals("double")) {
							stringBuilder.append( "\t" + nameVariable ).append(cardinalityString).append("=").append("1.54;\n");
						} else {
							stringBuilder.append( "\t" + nameVariable ).append(cardinalityString).append("=").append("\"dummy").append(typeDefinition.id()).append("\"").append(";\n");
						}
					}
				}
			}
		}
	}
}
