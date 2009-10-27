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

package joliex.java;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.Program;
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

		/*writer.write( "import jolie." );
		

		for( InterfaceDefinition i : interfaceDefinitions ) {
			convertInterfaceDefinition( i, writer );
		}

		writer.flush();
		 */
		// TODO: implement the conversion!
	}

	private void convertInterfaceDefinition( InterfaceDefinition iface, Writer writer )
		throws IOException
	{
		/*TypeDefinition type;

		for( Entry< String, OperationDeclaration > entry : iface.operationsMap().entrySet() ) {
			if ( entry.getValue() instanceof OneWayOperationDeclaration ) { // It's a One-Way
				type = ((OneWayOperationDeclaration)entry.getValue()).requestType();
			} else { // It's a Request-Response
				type = ((RequestResponseOperationDeclaration)entry.getValue()).requestType();
			}
		}*/
	}
}
