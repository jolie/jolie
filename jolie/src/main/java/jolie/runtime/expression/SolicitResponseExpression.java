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

package jolie.runtime.expression;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.events.OperationCallEvent;
import jolie.monitoring.events.OperationReplyEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.process.TransformationReason;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class SolicitResponseExpression implements Expression {
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression; // may be null
	private final RequestResponseTypeDescription types;
	private final ParsingContext context;
	private final VariablePath inputVarPath; // may be null

	public SolicitResponseExpression(
		String operationId,
		OutputPort outputPort,
		Expression outputExpression,
		RequestResponseTypeDescription types,
		ParsingContext context ) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.types = types;
		this.context = context;
		this.inputVarPath = null;
	}

	public SolicitResponseExpression(
		String operationId,
		OutputPort outputPort,
		Expression outputExpression,
		RequestResponseTypeDescription types,
		ParsingContext context,
		VariablePath inputVarPath ) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.types = types;
		this.context = context;
		this.inputVarPath = inputVarPath;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new SolicitResponseExpression(
			operationId,
			outputPort,
			(outputExpression == null) ? null : outputExpression.cloneExpression( reason ),
			types,
			context );
	}

	private void log( String log, CommMessage message ) {
		final Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.SOLICIT_RESPONSE,
			operationId + "@" + outputPort.id(),
			log,
			message,
			context ) );
	}


	@Override
	public Value evaluate()
		throws FaultException.RuntimeFaultException {
		CommChannel channel = null;
		CommMessage response = null;
		try {
			CommMessage message =
				CommMessage.createRequest(
					operationId,
					outputPort.getResourcePath(),
					(outputExpression == null) ? Value.UNDEFINED_VALUE : outputExpression.evaluate() );

			log( "SENDING", message );
			if( types.requestType() != null ) {
				try {
					types.requestType().check( message.value() );
				} catch( TypeCheckingException e ) {
					log( "TYPE MISMATCH", message );
					// just for logging also cause
					Value tmpValue = Value.create();
					tmpValue.setValue( e.getMessage() );
					log( "TYPE MISMATCH", new CommMessage( message.requestId(), message.operationName(),
						message.resourcePath(), tmpValue, null ) );
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new OperationCallEvent( operationId, ExecutionThread.currentThread().getSessionId(),
								Long.toString( message.requestId() ), OperationCallEvent.FAULT,
								"TypeMismatch:" + e.getMessage(), outputPort.id(), message.value(),
								Long.toString( message.id() ) ) );
					}

					throw e;
				}
			}

			channel = outputPort.getCommChannel();
			channel.send( message );
			// channel.release(); TODO release channel if possible (i.e. it will not be closed)
			log( "SENT", message );
			if( Interpreter.getInstance().isMonitoring() ) {
				Interpreter.getInstance()
					.fireMonitorEvent( new OperationCallEvent( operationId,
						ExecutionThread.currentThread().getSessionId(), Long.toString( message.requestId() ),
						OperationCallEvent.SUCCESS, "", outputPort.id(), message.value(),
						Long.toString( message.id() ) ) );
			}

			do {
				try {
					response = channel.recvResponseFor( message ).get( Interpreter.getInstance().responseTimeout(),
						TimeUnit.MILLISECONDS );
				} catch( InterruptedException e ) {
					throw new IOException( e );
				} catch( ExecutionException e ) {
					if( e.getCause() instanceof IOException ) {
						throw (IOException) e.getCause();
					} else {
						throw new IOException( e.getCause() );
					}
				}
			} while( response == null );
			log( "RECEIVED", response );

			if( inputVarPath != null ) {
				inputVarPath.setValue( response.value() );
			}

			if( response.isFault() ) {
				Type faultType = types.getFaultType( response.fault().faultName() );
				if( faultType != null ) {
					try {
						faultType.check( response.fault().value() );
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance()
								.fireMonitorEvent( new OperationReplyEvent( operationId,
									ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.requestId() ), OperationReplyEvent.FAULT,
									response.fault().faultName(), outputPort.id(), response.fault().value(),
									Long.toString( response.id() ) ) );
						}
					} catch( TypeCheckingException e ) {
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance()
								.fireMonitorEvent( new OperationReplyEvent( operationId,
									ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.requestId() ), OperationReplyEvent.FAULT,
									"TypeMismatch on fault:" + response.fault().faultName() + "." + e.getMessage(),
									outputPort.id(), response.fault().value(),
									Long.toString( response.id() ) ) );
						}
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
							"Received fault " + response.fault().faultName() + " TypeMismatch (" + operationId + "@"
								+ outputPort.id() + "): " + e.getMessage() ).toRuntimeFaultException();
					}
				} else {
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(),
								Long.toString( response.requestId() ), OperationReplyEvent.FAULT,
								response.fault().faultName(), outputPort.id(), response.fault().value(),
								Long.toString( response.id() ) ) );
					}
				}
				throw response.fault();
			} else {
				if( types.responseType() != null ) {
					try {
						types.responseType().check( response.value() );
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance()
								.fireMonitorEvent( new OperationReplyEvent( operationId,
									ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.requestId() ), OperationReplyEvent.SUCCESS, "",
									outputPort.id(), response.value(),
									Long.toString( response.id() ) ) );
						}
					} catch( TypeCheckingException e ) {
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance()
								.fireMonitorEvent( new OperationReplyEvent( operationId,
									ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.requestId() ), OperationReplyEvent.FAULT, e.getMessage(),
									outputPort.id(), response.value(),
									Long.toString( response.id() ) ) );
						}
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received message TypeMismatch ("
							+ operationId + "@" + outputPort.id() + "): " + e.getMessage() ).toRuntimeFaultException();
					}
				} else {
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent( new OperationReplyEvent( operationId,
							ExecutionThread.currentThread().getSessionId(), Long.toString( response.requestId() ),
							OperationReplyEvent.SUCCESS, "", outputPort.id(), response.value(),
							Long.toString( response.id() ) ) );
					}
				}
			}
		} catch( TimeoutException e ) { // The response timed out
			throw new FaultException( Constants.TIMEOUT_EXCEPTION_FAULT_NAME ).toRuntimeFaultException();
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ).toRuntimeFaultException();
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
				"Output message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() )
					.toRuntimeFaultException();
		} catch( FaultException e ) {
			throw e.toRuntimeFaultException();
		} finally {
			if( channel != null ) {
				try {
					channel.release();
				} catch( IOException e ) {
					Interpreter.getInstance().logWarning( e );
				}
			}
		}

		if( response == null ) {
			throw new AssertionError( "The value returned by a solicit-response expression should never be null" );
		}
		return response.value();
	}
}
