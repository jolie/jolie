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

import jolie.ExecutionThread;
import jolie.runtime.Expression;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InvalidIdException;
import jolie.runtime.Value;

/** Assigns an expression value to a Variable.
 * @author Fabrizio Montesi
 */
public class AssignmentProcess implements Process, Expression
{
	private GlobalVariablePath varPath;
	private Expression expression;

	/** Constructor.
	 * 
	 * @param var the variable which will receive the value.
	 * @param expression the expression of which the evaluation will be stored in the variable.
	 * @throws InvalidIdException if varId does not identify a variable.
	 */
	public AssignmentProcess( GlobalVariablePath varPath, Expression expression )
	{
		this.varPath = varPath;
		this.expression = expression;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new AssignmentProcess( varPath, expression );
	}
	
	/** Evaluates the expression and stores its value in the variable. */
	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
		varPath.getValue().assignValue( expression.evaluate() );
	}
	
	public Value evaluate()
	{
		Value val = varPath.getValue();
		val.assignValue( expression.evaluate() );
		return val;
	}
}
