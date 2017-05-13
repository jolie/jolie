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
	
	private class SolicitResponseOnReceiveBehaviour implements UnkillableBehaviour {
		private final CommMessage message;
		private final CommChannel channel;

		public SolicitResponseOnReceiveBehaviour( CommChannel channel, CommMessage message )
		{
			this.message = message;
			this.channel = channel;
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			try {
				CommMessage response = channel.recvResponseFor( ctx, message );
				
				if ( response == null ) {
					ctx.executeNext( this );
					ctx.pauseExecution();
					return;
				}

				log( ctx, "RECEIVED", response );

				if ( inputVarPath != null )	 {
					inputVarPath.setValue( ctx, response.value() );
				}

				if ( response.isFault() ) {				
					Type faultType = types.getFaultType( response.fault().faultName() );
					if ( faultType != null ) {
						try {
							faultType.check( response.fault().value() );
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( ctx, operationId, Long.toString(response.id()), OperationReplyEvent.FAULT, response.fault().faultName(), outputPort.id(), response.fault().value() ) );
							}
						} catch( TypeCheckingException e ) {
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( ctx, operationId, Long.toString(response.id()), OperationReplyEvent.FAULT, "TypeMismatch on fault:" + response.fault().faultName() + "." + e.getMessage(), outputPort.id(), response.fault().value() ) );
							}
							throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received fault " + response.fault().faultName() + " TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
						}
					} else {
						if ( ctx.interpreter().isMonitoring() ) {
							ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( ctx, operationId, Long.toString(response.id()), OperationReplyEvent.FAULT, response.fault().faultName(), outputPort.id(), response.fault().value() ) );
						}
					}
					throw response.fault();
				} else {
					if ( types.responseType() != null ) {
						try {
							types.responseType().check( response.value() );
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( ctx, operationId, Long.toString(response.id()), OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value() ) );
							}
						} catch( TypeCheckingException e ) {
							if ( ctx.interpreter().isMonitoring() ) {
								ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( ctx, operationId, Long.toString(response.id()), OperationReplyEvent.FAULT, e.getMessage(), outputPort.id(), response.value() ) );
							}
							throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
						}					
					} else {
						if ( ctx.interpreter().isMonitoring() ) {
							ctx.interpreter().fireMonitorEvent( new OperationReplyEvent( ctx, operationId, Long.toString(response.id()), OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value() ) );
						}
					}
				}

				ctx.executeNext( installProcess );
			} catch( IOException e ) {
				throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
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
	public void run( StatefulContext ctx )
		throws FaultException
	{
		if ( ctx.isKilled() ) {
			return;
		}

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
						ctx.interpreter().fireMonitorEvent( new OperationCallEvent( ctx, operationId, Long.toString(message.id()), OperationCallEvent.FAULT, "TypeMismatch:" + e.getMessage(), outputPort.id(), message.value() ) );
					}
					throw( e );
				}
			}
			
			CommChannel channel = outputPort.getCommChannel( ctx );
			
			log( ctx, "SENDING", message );
			
			ctx.executeNext( new SolicitResponseOnReceiveBehaviour( channel, message ) );
			
			channel.registerWaiterFor( ctx, message );
			channel.send( ctx, message, () -> {
				log( ctx, "SENT", message );
				if ( ctx.interpreter().isMonitoring() ) {
					ctx.interpreter().fireMonitorEvent( new OperationCallEvent( ctx, operationId, Long.toString ( message.id() ), OperationCallEvent.SUCCESS, "", outputPort.id(), message.value() ) );
				}
			}, f -> {} );
			
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			ctx.interpreter().logSevere( e );
		} catch( TypeCheckingException e ) {			
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Output message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}