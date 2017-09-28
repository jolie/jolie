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

package jolie.process;

import java.io.IOException;
import java.net.URISyntaxException;
import jolie.ExecutionThread;
import jolie.Interpreter;
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

public class SolicitResponseProcess implements Process
{
	private final String operationId;
	private final OutputPort outputPort;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Process installProcess; // may be null
	private final RequestResponseTypeDescription types;

	public SolicitResponseProcess(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression,
			VariablePath inputVarPath,
			Process installProcess,
			RequestResponseTypeDescription types
	) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
		this.types = types;
	}

	public Process clone( TransformationReason reason )
	{
		return new SolicitResponseProcess(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason ),
					( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
					( installProcess == null ) ? null : installProcess.clone( reason ),
					types
				);
	}

	private void log( String log, CommMessage message )
	{
		final Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new MessageTraceAction(
                        ExecutionThread.currentThread().getSessionId(),
			MessageTraceAction.Type.SOLICIT_RESPONSE,
			operationId + "@" + outputPort.id(),
			log,
			message,
                        System.currentTimeMillis()
		) );
	}

	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
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

			log( "SENDING", message );
			if ( types.requestType() != null ) {
				try {
					types.requestType().check( message.value() );
				} catch ( TypeCheckingException e ) {
					if ( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent( new OperationCallEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( message.id()).toString(), OperationCallEvent.FAULT, "TypeMismatch:" + e.getMessage(), outputPort.id(), message.value() ) );
					}

					throw( e );
				}
			}

			channel = outputPort.getCommChannel();
			channel.send( message );
			//channel.release(); TODO release channel if possible (i.e. it will not be closed)
			log( "SENT", message );
			if ( Interpreter.getInstance().isMonitoring() ) {
				Interpreter.getInstance().fireMonitorEvent( new OperationCallEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.toString ( message.id() ), OperationCallEvent.SUCCESS, "", outputPort.id(), message.value() ) );
			}

			CommMessage response = null;
			do {
				response = channel.recvResponseFor( message );
			} while( response == null );
			log( "RECEIVED", response );

			if ( inputVarPath != null )	 {
				inputVarPath.setValue( response.value() );
			}

			if ( response.isFault() ) {
				Type faultType = types.getFaultType( response.fault().faultName() );
				if ( faultType != null ) {
					try {
						faultType.check( response.fault().value() );
						if ( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, response.fault().faultName(), outputPort.id(), response.fault().value() ) );
						}
					} catch( TypeCheckingException e ) {
						if ( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, "TypeMismatch on fault:" + response.fault().faultName() + "." + e.getMessage(), outputPort.id(), response.fault().value() ) );
						}
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received fault " + response.fault().faultName() + " TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
					}
				} else {
					if ( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, response.fault().faultName(), outputPort.id(), response.fault().value() ) );
					}
				}
				throw response.fault();
			} else {
				if ( types.responseType() != null ) {
					try {
						types.responseType().check( response.value() );
						if ( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value() ) );
						}
					} catch( TypeCheckingException e ) {
						if ( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.FAULT, e.getMessage(), outputPort.id(), response.value() ) );
						}
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
					}
				} else {
					if ( Interpreter.getInstance().isMonitoring() ) {
				        Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(), Long.valueOf( response.id()).toString(), OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value() ) );
			        }
				}
			}

			try {
				installProcess.run();
			} catch( ExitingException e ) { assert false; }
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Output message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
		} finally {
			if ( channel != null ) {
				try {
					channel.release();
				} catch( IOException e ) {
					Interpreter.getInstance().logWarning( e );
				}
			}
		}
	}

	public boolean isKillable()
	{
		return true;
	}
}
