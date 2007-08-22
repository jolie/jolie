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


package jolie.runtime;

import jolie.lang.parse.Scanner;


/**
 * @author Fabrizio Montesi
 * @todo Clean up code.
 *
 */
public class CompareCondition implements Condition
{
	private Expression leftExpression, rightExpression;
	private Scanner.TokenType opType;
	
	public CompareCondition( Expression left, Expression right, Scanner.TokenType opType )
	{
		leftExpression = left;
		rightExpression = right;
		this.opType = opType;
	}
	
	public boolean evaluate()
	{
		boolean retval = false;
		Value leftVal, rightVal;
		leftVal = leftExpression.evaluate();
		rightVal = rightExpression.evaluate();
		
		if ( opType == Scanner.TokenType.EQUAL ) {
			if ( leftVal.isString() && rightVal.isString() )
				retval = leftVal.strValue().equals( rightVal.strValue() );
			else
				retval = ( leftVal.intValue() == rightVal.intValue() );
		} else if ( opType == Scanner.TokenType.NOT_EQUAL ) {
			if ( leftVal.isString() && rightVal.isString() )
				retval = !(leftVal.strValue().equals( rightVal.strValue() ));
			else
				retval = ( leftVal.intValue() != rightVal.intValue() );
		} else if ( opType == Scanner.TokenType.LANGLE )
			retval = ( leftVal.intValue() < rightVal.intValue() );
		else if ( opType == Scanner.TokenType.RANGLE )
			retval = ( leftVal.intValue() > rightVal.intValue() );
		else if ( opType == Scanner.TokenType.MINOR_OR_EQUAL )
			retval = ( leftVal.intValue() <= rightVal.intValue() );
		else if ( opType == Scanner.TokenType.MAJOR_OR_EQUAL )
			retval = ( leftVal.intValue() >= rightVal.intValue() );
		
		return retval;
	}
}
