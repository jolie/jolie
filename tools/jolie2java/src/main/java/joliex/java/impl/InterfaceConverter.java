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

package joliex.java.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.logging.Logger;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import joliex.java.impl.InterfaceVisitor;
import joliex.java.impl.InterfaceVisitor.InterfaceNotFound;

/**
 *
 * @author Fabrizio Montesi
 */
public class InterfaceConverter
{
	private final Program program;
	private final String[] interfaceNames;
	private final Logger logger;
	private Writer writer;
	private int indentationLevel = 0;

	public InterfaceConverter( Program program, String[] interfaceNames, Logger logger )
	{
		this.program = program;
		this.interfaceNames = interfaceNames;
		this.logger = logger;
	}

	private void indent()
	{
		indentationLevel++;
	}

	private void unindent()
	{
		indentationLevel--;
	}

	public void convert( Writer writer )
		throws InterfaceNotFound, IOException
	{
		this.writer = writer;
		InterfaceDefinition[] interfaceDefinitions =
			new InterfaceVisitor( program, interfaceNames ).getInterfaceDefinitions();

		writeHeader();
		indent();
		for( InterfaceDefinition i : interfaceDefinitions ) {
			convertInterfaceDefinition( i );
		}
		unindent();
		writeFooter();

		writer.flush();
	}

	private void writeFooter()
		throws IOException
	{
		writeLine( "}" );
	}

	private void writeHeader()
		throws IOException
	{
		String className = "Tmp";
		writeLine( "public class " + className );
		writeLine( "{" );
		indent();
		writeLine( "private final JolieAdapter service;\n" );
		writeLine( "public " + className + "( JavaService javaService )" );
		writeLine( "{" );
		indent();
		writeLine( "this.service = new JolieAdapter( javaService, \"/\" );" );
		unindent();
		writeLine( "}" );
		unindent();
	}

	private void writeLine( String s )
		throws IOException
	{
		for( int i = 0; i < indentationLevel; i++ ) {
			writer.write( "\t" );
		}
		writer.write( s );
		writer.write( "\n" );
	}

	private void convertInterfaceDefinition( InterfaceDefinition iface )
		throws IOException
	{
		for( Entry< String, OperationDeclaration > entry : iface.operationsMap().entrySet() ) {
			writeLine( "" );
			if ( entry.getValue() instanceof OneWayOperationDeclaration ) { // It's a One-Way
				writeOperation( (OneWayOperationDeclaration)entry.getValue() );
			} else { // It's a Request-Response
				writeOperation( (RequestResponseOperationDeclaration)entry.getValue() );
			}
		}
	}

	private void writeOperation( OneWayOperationDeclaration op )
		throws IOException
	{
		writeLine( "public void " + op.id() + "( Value request )" );
		writeLine( "{" );
		indent();
		writeLine( "service.callOneWay( \"" + op.id() + "\", request );" );
		unindent();
		writeLine( "}" );
	}

	private void writeOperation( RequestResponseOperationDeclaration op )
		throws IOException
	{
		writeLine( "public Value " + op.id() + "( Value request )" );
		indent(); writeLine( "throws FaultException"); unindent();
		writeLine( "{" );
		indent();
		writeLine( "return service.callRequestResponse( \"" + op.id() + "\", request );" );
		unindent();
		writeLine( "}" );
	}
}
