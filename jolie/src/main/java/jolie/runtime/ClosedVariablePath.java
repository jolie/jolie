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

package jolie.runtime;

import jolie.runtime.expression.Expression;
import jolie.process.TransformationReason;
import jolie.util.Pair;

/**
 * @author Fabrizio Montesi
 */
public class ClosedVariablePath extends VariablePath
{
	private final Value rootValue;

	public ClosedVariablePath( Pair< Expression, Expression >[] path, Value rootValue )
	{
		super( path );
		this.rootValue = rootValue;
	}
	
	public ClosedVariablePath( VariablePath otherPath, Value rootValue )
	{
		this( otherPath.path(), rootValue );
	}

	@Override
	protected VariablePath _createVariablePath( Pair< Expression, Expression >[] path )
	{
		return new ClosedVariablePath( path, rootValue );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason )
	{
		Pair< Expression, Expression >[] clonedPath = cloneExpressionHelper( path(), reason );
		return new ClosedVariablePath( clonedPath, rootValue );
	}

	@Override
	protected Value getRootValue()
	{
		return rootValue;
	}
}
