/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package joliex.plasma;

import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.logging.Logger;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.types.*;
import joliex.plasma.impl.InterfaceVisitor;
import joliex.plasma.impl.InterfaceVisitor.InterfaceNotFound;

/**
 *
 * @author Fabrizio Montesi
 */
public class InterfaceConverter
{
	private final Program program;
	private final String[] interfaceNames;
	private final Logger logger;

	public InterfaceConverter( Program program, String[] interfaceNames, Logger logger )
	{
		this.program = program;
		this.interfaceNames = interfaceNames;
		this.logger = logger;
	}

	public void convert( Writer writer )
		throws InterfaceNotFound, IOException
	{
		InterfaceDefinition[] interfaceDefinitions =
			new InterfaceVisitor( program, interfaceNames ).getInterfaceDefinitions();

		writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
		writer.write( "<!DOCTYPE kcfg SYSTEM\n\t\"http://www.kde.org/standards/kcfg/1.0/kcfg.xsd\">\n" );
		writer.write( "<kcfg>\n" );

		for( InterfaceDefinition i : interfaceDefinitions ) {
			convertInterfaceDefinition( i, writer );
		}

		writer.write( "</kcfg>\n" );
		writer.flush();
	}

	private void unsupported( InterfaceDefinition iface, String operationName, String message )
	{
		logger.severe( iface.name() + "." + operationName + " uses a feature unsupported by plasma service specifications: " + message );
	}

	private void convertInterfaceDefinition( InterfaceDefinition iface, Writer writer )
		throws IOException
	{
		TypeDefinition type;

		for( Entry< String, OperationDeclaration > entry : iface.operationsMap().entrySet() ) {
			if ( entry.getValue() instanceof OneWayOperationDeclaration ) { // It's a One-Way
				type = ( ( OneWayOperationDeclaration )entry.getValue() ).requestType();
			} else { // It's a Request-Response
				type = ( ( RequestResponseOperationDeclaration )entry.getValue() ).requestType();
			}

			writer.write( "\t<group name=\"" + entry.getKey() + "\"" );
			if ( type == null || type.equals( TypeDefinitionUndefined.getInstance() ) ) {
				writer.write( " />\n" );
				unsupported( iface, entry.getKey(), "undefined request type" );
			} else {
				writeType( iface, entry.getKey(), type, writer );
			}
		}
	}

	private void writeEntry( String entryName, String entryType, Writer writer )
		throws IOException
	{
		writer.write( "\t\t<entry name=\"" + entryName + "\" type=\"" + entryType + "\" />\n" );
	}

	private void writeType( InterfaceDefinition iface, String operationName, TypeDefinition type, Writer writer )
		throws IOException
	{
		if ( type instanceof TypeInlineDefinition ) {
			boolean empty = true;

			if ( ( ( TypeInlineDefinition ) type ).nativeType() != NativeType.VOID ) {
				empty = false;
				writer.write( ">\n" );
				writeEntry( "/", getPlasmaParameterType( iface, operationName, "/", type ), writer );
			}

			if ( ( ( TypeInlineDefinition ) type ).hasSubTypes() ) {
				if ( empty ) {
					writer.write( ">\n" );
				}
				writeParameters( iface, operationName, ( TypeInlineDefinition ) type, writer);
				writer.write( "\t</group>\n" );
			} else {
				if ( empty ) {
					writer.write( " />\n" );
				} else {
					writer.write( "\t</group>\n" );
				}
			}
		} else if ( type instanceof TypeDefinitionLink ){
			writeType( iface, operationName, ( ( TypeDefinitionLink ) type ).linkedType(), writer );

		} else if ( type instanceof TypeChoiceDefinition ){
			writeType( iface, operationName, ( ( TypeChoiceDefinition ) type ).left(), writer );
		}
	}

	private void writeParameters( InterfaceDefinition iface, String operationName, TypeInlineDefinition type, Writer writer )
		throws IOException
	{
		String plasmaType;
		for( Entry< String, TypeDefinition > entry : type.subTypes() ) {
			plasmaType = getPlasmaParameterType( iface, operationName, entry.getKey(), entry.getValue() );
			writeEntry( entry.getKey(), plasmaType, writer );
		}
	}

	private String getPlasmaParameterType(
		InterfaceDefinition iface,
		String operationName,
		String parameterName,
		TypeDefinition type
	) {
		String ret = "unsupported";
		if ( type instanceof TypeInlineDefinition ) {
			if ( ( (TypeInlineDefinition ) type ).hasSubTypes() ) {
				// TODO check for sub types that can be represented by a Qt class, like QRect or QPoint
				unsupported( iface, operationName, "nested sub-elements under parameter " + parameterName );
			}

			NativeType nativeType = ( ( TypeInlineDefinition ) type ).nativeType();
			if ( nativeType == NativeType.VOID ) {
				unsupported( iface, operationName, "void native type for parameter " + parameterName );
			} else if ( nativeType == NativeType.INT ) {
				ret = "Int";
			} else if ( nativeType == NativeType.DOUBLE ) {
				ret = "Double";
			} else if ( nativeType == NativeType.STRING ) {
				ret = "String";
			} else if ( nativeType == NativeType.ANY ) {
				unsupported( iface, operationName, "any native type for parameter " + parameterName );
			}

		} else if ( type instanceof TypeDefinitionLink ) {
			ret = getPlasmaParameterType( iface, operationName, parameterName, ( ( TypeDefinitionLink ) type ).linkedType() );

		} else if ( type instanceof TypeChoiceDefinition ){
			ret = getPlasmaParameterType( iface, operationName, parameterName, ( ( TypeChoiceDefinition ) type ).left() );
		}

		return ret;
	}
}
