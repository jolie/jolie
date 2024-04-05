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


import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;

public class RunProcess implements Process {
	private final Expression expression;

	public RunProcess( Expression expression ) {
		this.expression = expression;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new RunProcess( expression.cloneExpression( reason ) );
	}

	@Override
	public void run()
		throws FaultException {
		throw new FaultException( "UnsupportedStatement" );
		/*
		 * if ( ExecutionThread.currentThread().isKilled() ) return; Value val = expression.evaluate(); if (
		 * val.isString() ) { try { String codeStr = val.strValue(); // for run-time code use the system's
		 * default charset (null) OLParser parser = new OLParser( new Scanner( new ByteArrayInputStream(
		 * codeStr.getBytes() ), "unknown", null ), Interpreter.getInstance().includePaths(),
		 * Interpreter.getInstance().parentClassLoader() ); Program program = parser.parse(); program = (new
		 * OLParseTreeOptimizer( program )).optimize(); SemanticVerifier semanticVerifier = new
		 * SemanticVerifier( program ); if ( !semanticVerifier.validate() ) { throw new FaultException(
		 * "fInvalidCode" ); }
		 * 
		 * Interpreter parentInterpreter = Interpreter.getInstance(); Interpreter runInterpreter = new
		 * Interpreter( parentInterpreter.args() ); (new OOITBuilder( runInterpreter, program,
		 * semanticVerifier.isConstantMap() )).build(); } catch( IOException e ) { throw new FaultException(
		 * "fInvalidCode" ); } catch( ParserException e ) { throw new FaultException( "fInvalidCode" ); }
		 * catch( CommandLineException e ) { throw new FaultException( "fInvalidCode" ); } } else throw new
		 * FaultException( "fInvalidCode" );
		 */
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
