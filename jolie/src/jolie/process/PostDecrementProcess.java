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
import jolie.Interpreter;
import jolie.runtime.expression.Expression;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.tracer.Tracer;
import jolie.tracer.VariableTraceAction;

public class PostDecrementProcess implements Process, Expression
{
	final private VariablePath path;

	public PostDecrementProcess( VariablePath varPath )
	{
		this.path = varPath;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new PostDecrementProcess( (VariablePath)path.cloneExpression( reason ) );
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		return new PostDecrementProcess( (VariablePath) path.cloneExpression( reason ) );
	}
	
	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
                String varTreeName="";
                for(int i=0; i<path.path().length;i++){
                    varTreeName += path.path()[i].key().evaluate().strValue();
                    if(i != path.path().length-1){
                        varTreeName += ".";
                    }
                }
		Value val = path.getValue();
                log(path.getStateIdentifier(),"POSTDECREMENTING",(path.getValue()==null) ? null : path.getValue().evaluate().strValue(), varTreeName);
		val.setValue( val.intValue() - 1 );
                log(path.getStateIdentifier(),"POSTDECREMENTED",(path.getValue()==null) ? null : path.getValue().evaluate().strValue(), varTreeName);
	}
	
	public Value evaluate()
	{
		Value val = path.getValue();
		Value orig = Value.create( val.intValue() );
		val.setValue( val.intValue() - 1 );
		return orig;
	}
	
	public boolean isKillable()
	{
		return true;
	}
        
         private void log(String instance, String behaviour, String value , String variableName)
	{
		final Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new VariableTraceAction(
                        instance,
			VariableTraceAction.Type.POSTDECREMENT,
                        behaviour,
                        value,
                        variableName,
                        System.currentTimeMillis()
		) );
	}
}
