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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map.Entry;
import jolie.lang.parse.ast.InputPortInfo;
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
	private StringBuilder stringBuilder;
	private File sourceFile;
	private final int MAX_ARRAY_ITEMS = 5;
	private final String mockFilename = "mock_main.ol";

	public JolieDummyDocumentCreator( ProgramInspector inspector, File sourceFile )
	{

		this.inspector = inspector;
		stringBuilder = new StringBuilder();
		this.sourceFile = sourceFile;
	}

	public void createDocument()
		throws FileNotFoundException, IOException
	{
		String fileContent = new String( Files.readAllBytes( sourceFile.toPath() ) );
		stringBuilder.append( "main {\n" );
		for( InputPortInfo inputPortInfo : inspector.getInputPorts() ) {
			for( OperationDeclaration operationDeclaration : inputPortInfo.operations() ) {
					convertOperation( operationDeclaration, stringBuilder );
			}
		}

		stringBuilder.append( "}" );
		int mainIndex = fileContent.indexOf("main");
		stringBuilder.insert(0, fileContent.substring(0, mainIndex) );
		BufferedWriter writer = new BufferedWriter( new FileWriter( mockFilename ) );
		writer.append( stringBuilder.toString() );
		writer.flush();
		writer.close();
	}

	private void convertOperation( OperationDeclaration operatationDeclaration, StringBuilder stringBuilder )
	{

		if ( operatationDeclaration instanceof RequestResponseOperationDeclaration ) {
			stringBuilder.append( "\n[ " ).append( operatationDeclaration.id() ).append( "( request )( response ){ \n" );
			TypeDefinition typeDefinition = ( ( RequestResponseOperationDeclaration ) operatationDeclaration ).responseType();
			convertTypes( typeDefinition, stringBuilder, "dummyResponse" );
			stringBuilder.append( "\tresponse << dummyResponse\n" );
			stringBuilder.append( "} ] { nullProcess }\n\n" );
		}

	}

	private void convertTypes( TypeDefinition typeDefinition, StringBuilder stringBuilder, String nameVariable ) {
		if ( typeDefinition instanceof TypeChoiceDefinition ) {
			TypeChoiceDefinition typeChoiceDefinition = ( TypeChoiceDefinition ) typeDefinition;
			convertTypes( typeChoiceDefinition.left(), stringBuilder, nameVariable );

		} else if ( typeDefinition instanceof TypeDefinitionLink ) {
			TypeDefinitionLink typeDefinitionLink = ( TypeDefinitionLink ) typeDefinition;
			convertTypes( typeDefinitionLink.linkedType(), stringBuilder, nameVariable );

		} else if ( typeDefinition instanceof TypeInlineDefinition ) {
			TypeInlineDefinition typeInlineDefinition = ( TypeInlineDefinition ) typeDefinition;
			//check subtypes
			if ( typeInlineDefinition.hasSubTypes() ) {
				for ( Entry<String, TypeDefinition> entry : typeInlineDefinition.subTypes() ) {
					convertTypes( entry.getValue(), stringBuilder, nameVariable + "." + entry.getKey() );
				}
			} else {
				//get cardinality indexes. if maximum cardinality is considerably big (more than 5), print only first 5 entries; or print less otherwise; don't print cardinality, if it equals 1
				int typeCardinality = ( typeInlineDefinition.cardinality().max() > MAX_ARRAY_ITEMS ? MAX_ARRAY_ITEMS : typeInlineDefinition.cardinality().max() );
				for ( int cardinalityIndex = 0; cardinalityIndex < typeCardinality; cardinalityIndex++ ) {
					String cardinalityString = typeCardinality == 1 ? "" : "[ " + String.valueOf( cardinalityIndex ) + " ]";
					if ( !( typeInlineDefinition.nativeType().id().equals( "void" ) ) ) {
						if ( typeInlineDefinition.nativeType().id().equals( "int" ) ) {
							stringBuilder.append( "\t" + nameVariable ).append( cardinalityString ).append( "=" ).append( "42;\n" );
						} else if ( typeInlineDefinition.nativeType().id().equals( "double" ) ) {
							stringBuilder.append( "\t" + nameVariable ).append( cardinalityString ).append( "=" ).append( "1.54;\n" );
						} else if ( typeInlineDefinition.nativeType().id().equals( "long" ) ) {
							stringBuilder.append( "\t" + nameVariable ).append( cardinalityString ).append( "=" ).append( "424242L;\n" );
						} else {
							stringBuilder.append( "\t" + nameVariable ).append( cardinalityString ).append( "=" ).append( "\"dummy" ).append( typeDefinition.id() ).append( "\"" ).append( ";\n" );
						}
					}
				}
			}
		}
	}
}
