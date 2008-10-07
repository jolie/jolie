/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.process;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.OOITBuilder;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class RunProcess implements Process
{
	final private Expression expression;
	
	public RunProcess( Expression expression )
	{
		this.expression = expression;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new RunProcess( expression.cloneExpression( reason ) );
	}
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
		Value val = expression.evaluate();
		if ( val.isString() ) {
			try {
				String codeStr = val.strValue();
				OLParser parser =
					new OLParser(
							new Scanner( new ByteArrayInputStream( codeStr.getBytes() ), "unknown" ),
							Interpreter.getInstance().includePaths(),
							Interpreter.getInstance().parentClassLoader()
						);
				Program program = parser.parse();
				program = (new OLParseTreeOptimizer( program )).optimize();
				if ( !(new SemanticVerifier( program )).validate() )
					throw new FaultException( "fInvalidCode" );
			
				(new OOITBuilder( Interpreter.getInstance(), program )).build();
			} catch( IOException ioe ) {
				throw new FaultException( "fInvalidCode" );
			} catch( ParserException ioe ) {
				throw new FaultException( "fInvalidCode" );
			}			
		} else
			throw new FaultException( "fInvalidCode" );
	}
}
