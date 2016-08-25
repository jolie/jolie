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

import jolie.SessionContext;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

public class DeepCopyProcess implements Process
{
	private final VariablePath leftPath;
	private final Expression rightExpression;

	public DeepCopyProcess( VariablePath leftPath, Expression rightExpression )
	{
		this.leftPath = leftPath;
		this.rightExpression = rightExpression;
	}

	@Override
	public Process clone( TransformationReason reason )
	{
		return new DeepCopyProcess(
					(VariablePath)leftPath.cloneExpression( reason ),
					rightExpression.cloneExpression( reason )
				);
	}

	@Override
	public void run(SessionContext ctx)
	{
		if ( ctx.isKilled() )
			return;

		if ( rightExpression instanceof VariablePath ) {
			leftPath.deepCopy( (VariablePath) rightExpression );
		} else {
			leftPath.getValue( ctx ).deepCopy( rightExpression.evaluate() );
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
