/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie.runtime.expression;

import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.util.Pair;


/**
 * Implements inline tree definitions.
 * @author Fabrizio Montesi
 */
public class InlineTreeExpression implements Expression
{
	private final Expression rootExpression;
	private final Pair< VariablePath, Expression >[] assignments;
	
	public InlineTreeExpression(
		Expression rootExpression,
		Pair< VariablePath, Expression >[] assignments
	) {
		this.rootExpression = rootExpression;
		this.assignments = assignments;
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		Pair< VariablePath, Expression >[] cloneAssignments = new Pair[ assignments.length ];
		int i = 0;
		for( Pair< VariablePath, Expression > pair : assignments ) {
			cloneAssignments[ i++ ] =
				new Pair< VariablePath, Expression >(
					pair.key().clone(),
					pair.value().cloneExpression( reason )
				);
		}
		
		return new InlineTreeExpression(
			rootExpression.cloneExpression( reason ),
			cloneAssignments
		);
	}
	
	public Value evaluate()
	{
		Value inlineValue = Value.create();
		inlineValue.assignValue( rootExpression.evaluate() );
		
		for( Pair< VariablePath, Expression > pair : assignments ) {
			pair.key().getValue( inlineValue ).assignValue( pair.value().evaluate() );
		}
		
		return inlineValue;
	}
}
