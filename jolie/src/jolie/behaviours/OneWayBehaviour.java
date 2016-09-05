/***************************************************************************
 *   Copyright (C) 2006-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.behaviours;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jolie.Interpreter;
import jolie.StatefulContext;
import jolie.monitoring.events.OperationStartedEvent;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.OneWayOperation;
import jolie.runtime.VariablePath;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class OneWayBehaviour implements InputOperationBehaviour
{
	private final OneWayOperation operation;
	private final VariablePath varPath;
	private boolean isSessionStarter = false;

	public OneWayBehaviour( OneWayOperation operation, VariablePath varPath )
	{
		this.operation = operation;
		this.varPath = varPath;
	}

	public void setSessionStarter( boolean isSessionStarter )
	{
		this.isSessionStarter = isSessionStarter;
	}
	
	public InputOperation inputOperation()
	{
		return operation;
	}
	
	public Behaviour clone( TransformationReason reason )
	{
		return new OneWayBehaviour( operation, varPath );
	}
	
	public VariablePath inputVarPath()
	{
		return varPath;
	}

	public Behaviour receiveMessage( final SessionMessage sessionMessage, StatefulContext ctx )
	{
		if ( ctx.interpreter().isMonitoring() && !isSessionStarter ) {
			ctx.interpreter().fireMonitorEvent( new OperationStartedEvent( operation.id(), ctx.getSessionId(), Long.toString(sessionMessage.message().id()), sessionMessage.message().value() ) );
		}

		log( ctx.interpreter(), "RECEIVED", sessionMessage.message() );
		if ( varPath != null ) {
			varPath.getValue( ctx.state().root() ).refCopy( sessionMessage.message().value() );
		}

		return NullBehaviour.getInstance();
	}

	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		Future< SessionMessage > f = ctx.requestMessage( operation, ctx );
		try {
			SessionMessage m = f.get();
			if ( m != null ) { // If it is null, we got killed by a fault
				ctx.executeNext( receiveMessage( m, ctx ) );
			}
		} catch( InterruptedException | ExecutionException e ) {
			ctx.interpreter().logSevere( e );
		}
	}

	private void log(Interpreter interpreter, String log, CommMessage message )
	{
		final Tracer tracer = interpreter.tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.ONE_WAY,
			operation.id(),
			log,
			message
		) );
	}

	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}
