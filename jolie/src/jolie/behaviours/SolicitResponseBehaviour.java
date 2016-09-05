/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import jolie.StatefulContext;
import jolie.lang.Constants;
import jolie.monitoring.events.OperationCallEvent;
import jolie.monitoring.events.OperationReplyEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class SolicitResponseBehaviour implements Behaviour
{
	private final String operationId;
	private final OutputPort outputPort;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Behaviour installProcess; // may be null
	private final RequestResponseTypeDescription types;
	
	private class SolicitResponseOnSendBehaviour implements Behaviour {
		
		private final CommMessage message;
		private final CommChannel channel;
		
		public SolicitResponseOnSendBehaviour(CommChannel channel, CommMessage message)
		{
			this.message = message;
			this.channel = channel;
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			System.out.println( "Sending of message is complete!" );
			log( ctx, "SENT", message );
			if ( ctx.interpreter().isMonitoring() ) {
				ctx.interpreter().fireMonitorEvent( new OperationCallEvent( operationId, ctx.getSessionId(), Long.toString ( message.id() ), OperationCallEvent.SUCCESS, "", outputPort.id(), message.value() ) );
			}
			ctx.pauseExecution();
			ctx.executeNext( new SolicitResponseOnReceiveBehaviour(channel, message) );
		}

		@Override
		public Behaviour clone( TransformationReason reason )
		{
			return new SolicitResponseOnSendBehaviour( channel, message );
		}

		@Override
		public boolean isKillable()
		{
			return false;
		}
		
	}

	private class SolicitResponseOnReceiveBehaviour implements Behaviour {
		
		private final CommMessage message;
		private final CommChannel channel;

		public SolicitResponseOnReceiveBehaviour(CommChannel channel, CommMessage message)
		{
			this.message = message;
			this.channel = channel;
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			System.out.println( "Start of handling response!" );
			try {
				CommMessage response = null;
				do {
					response = channel.recvResponseFor( message );
				} while( response == null );

				log( ctx, "RECEIVED", response );

				if ( inputVarPath != null )	 {
					inputVarPath.setValue( response.value() );
				}

				if ( response.isFault() ) {				
					Type faultType = types.getFaultType( response.fault().faultName() );
					if ( faultType != null ) {
						try {
							faultType.check( response.fault().value() );
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( operationId, ctx.getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, response.fault().faultName(), outputPort.id(), response.fault().value() ) );
							}
						} catch( TypeCheckingException e ) {
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( operationId, ctx.getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, "TypeMismatch on fault:" + response.fault().faultName() + "." + e.getMessage(), outputPort.id(), response.fault().value() ) );
							}
							throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received fault " + response.fault().faultName() + " TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
						}
					} else {
						if ( ctx.interpreter().isMonitoring() ) {
							ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( operationId, ctx.getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, response.fault().faultName(), outputPort.id(), response.fault().value() ) );
						}
					}
					throw response.fault();
				} else {
					if ( types.responseType() != null ) {
						try {
							types.responseType().check( response.value() );
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( operationId, ctx.getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value() ) );
							}
						} catch( TypeCheckingException e ) {
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( operationId, ctx.getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, e.getMessage(), outputPort.id(), response.value() ) );
							}
							throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
						}					
					} else {
						if ( ctx.interpreter().isMonitoring() ) {
							ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( operationId, ctx.getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value() ) );
						}
					}
				}

				try {
					installProcess.run( ctx );
				} catch( ExitingException e ) { assert false; }
			} catch( IOException e ) {
				throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
			}
		}

		@Override
		public Behaviour clone( TransformationReason reason )
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		@Override
		public boolean isKillable()
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}
		
	}
	
	public SolicitResponseBehaviour(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression,
			VariablePath inputVarPath,
			Behaviour installProcess,
			RequestResponseTypeDescription types
	) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
		this.types = types;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new SolicitResponseBehaviour(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason ),
					( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
					( installProcess == null ) ? null : installProcess.clone( reason ),
					types
				);
	}

	private void log( StatefulContext ctx,  String log, CommMessage message )
	{
		final Tracer tracer = ctx.interpreter().tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.SOLICIT_RESPONSE,
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
				CommMessage.createRequest(
					operationId,
					outputPort.getResourcePath(),
					( outputExpression == null ) ? Value.UNDEFINED_VALUE : outputExpression.evaluate()
				);

			if ( types.requestType() != null ) {
				try {
					types.requestType().check( message.value() );
				} catch ( TypeCheckingException e ) {
					if ( ctx.interpreter().isMonitoring() ) {
						ctx.interpreter().fireMonitorEvent( new OperationCallEvent( operationId, ctx.getSessionId(), Long.toString(message.id()), OperationCallEvent.FAULT, "TypeMismatch:" + e.getMessage(), outputPort.id(), message.value() ) );
					}
					throw( e );
				}
			}

			channel = outputPort.getCommChannel( ctx );
			log( ctx, "SENDING", message );
			
			// Make sure completion handler is registered before sending, to avoid racecondition.
			ctx.executeNext( new SolicitResponseOnSendBehaviour( channel, message) );
			
			channel.send( message, ctx );
			//channel.release(); TODO release channel if possible (i.e. it will not be closed)
			
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			ctx.interpreter().logSevere( e );
		} catch( TypeCheckingException e ) {			
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Output message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
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