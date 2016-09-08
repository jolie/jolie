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

package jolie.behaviours;

import java.io.IOException;
import java.net.URISyntaxException;
import jolie.Interpreter;
import jolie.StatefulContext;
import jolie.lang.Constants;
import jolie.monitoring.events.OperationCallEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class NotificationBehaviour implements Behaviour
{
	private class NotificationOnAckBehaviour implements Behaviour {
		
		private final CommChannel channel;
		private final CommMessage message;

		public NotificationOnAckBehaviour( CommChannel channel, CommMessage message )
		{
			this.channel = channel;
			this.message = message;
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			try {
				CommMessage response = channel.recvResponseFor( message );
				if ( response == null) {
					System.out.println( "did not recieve ack yet go to sleep." );
					ctx.executeNext( this );
					ctx.pauseExecution();
					return;
				}

				log( ctx.interpreter(), "RECEIVED ACK", response );

				if ( response.isFault() ) {
					if ( response.fault().faultName().equals( "CorrelationError" )
						|| response.fault().faultName().equals( "IOException" )
						|| response.fault().faultName().equals( "TypeMismatch" )
						) {
						throw response.fault();
					} else {
						ctx.interpreter().logSevere( "Notification process for operation " + operationId + " received an unexpected fault: " + response.fault().faultName() );
					}
				}

			} catch( IOException e ) {
				throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
			} 
		}

		@Override
		public Behaviour clone( TransformationReason reason )
		{
			return new NotificationOnAckBehaviour( channel, message );
		}

		@Override
		public boolean isKillable()
		{
			return true;
		}
		
	}
	
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression; // may be null
	private final OneWayTypeDescription oneWayDescription; // may be null

	public NotificationBehaviour(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression,
			OneWayTypeDescription outputType
			)
	{
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.oneWayDescription = outputType;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new NotificationBehaviour(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason ),
					oneWayDescription
				);
	}

	private void log( Interpreter interpreter, String log, CommMessage message )
	{
		final Tracer tracer = interpreter.tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.NOTIFICATION,
			operationId + "@" + outputPort.id(),
			log,
			message
		) );
	}

	@Override
	public void run(StatefulContext ctx)
		throws FaultException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		CommChannel channel = null;
		try {
			CommMessage message =
				( outputExpression == null ) ?
						CommMessage.createRequest( operationId, outputPort.getResourcePath(), Value.UNDEFINED_VALUE ) :
						CommMessage.createRequest( operationId, outputPort.getResourcePath(), outputExpression.evaluate() );
			if ( oneWayDescription != null ) {
				try  {
				oneWayDescription.requestType().check( message.value() );
				} catch( TypeCheckingException e ) {
					if ( ctx.interpreter().isMonitoring() ) {
						ctx.interpreter().fireMonitorEvent( new OperationCallEvent( operationId, ctx.getSessionId(), Long.toString(message.id()), OperationCallEvent.FAULT, "TypeMismatch:" + e.getMessage(), outputPort.id(), message.value() ) );
					}
					throw( e );
				}
			}
			channel = outputPort.getCommChannel( ctx );

			log( ctx.interpreter(), "SENDING", message );

			ctx.executeNext(new NotificationOnAckBehaviour(channel, message) );
			
			channel.send( message, ctx, ( Void ) -> {
				log( ctx.interpreter(), "SENT", message );
				if ( ctx.interpreter().isMonitoring() ) {
					ctx.interpreter().fireMonitorEvent( new OperationCallEvent( operationId, ctx.getSessionId(), Long.toString(message.id()), OperationCallEvent.SUCCESS, "", outputPort.id(), message.value() ) );
				}
				return null;
			});
			
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			ctx.interpreter().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
		} finally {
			if ( channel != null ) {
				try {
					channel.release();
				} catch( IOException e ) {
					ctx.interpreter().logWarning( e );
				}
			}
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}