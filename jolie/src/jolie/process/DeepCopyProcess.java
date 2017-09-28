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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.ExecutionThread;
import jolie.runtime.Value;
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

	public Process clone( TransformationReason reason )
	{
		return new DeepCopyProcess(
					(VariablePath)leftPath.cloneExpression( reason ),
					rightExpression.cloneExpression( reason )
				);
	}

	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;

		if ( rightExpression instanceof VariablePath ) {  
                    
			leftPath.deepCopy( (VariablePath) rightExpression );
		} else {
			leftPath.getValue().deepCopy( rightExpression.evaluate() );
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
