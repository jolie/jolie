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
import jolie.monitoring.events.OperationCallEvent;
import jolie.monitoring.events.OperationReplyEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.process.TransformationReason;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class SolicitResponseExpression implements Expression {
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression;
	private final RequestResponseTypeDescription types;

	public SolicitResponseExpression( String operationId, OutputPort outputPort, Expression outputExpression,
		RequestResponseTypeDescription types ) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.types = types;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new SolicitResponseExpression( operationId, outputPort, outputExpression, types );
	}

	@Override
	public Value evaluate() {
		try {
			CommMessage message =
				CommMessage.createRequest( operationId, outputPort.getResourcePath(), outputExpression.evaluate() );


			if( types.requestType() != null ) {
				try {
					types.requestType().check( message.value() );
				} catch( TypeCheckingException e ) {
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new OperationCallEvent( operationId, ExecutionThread.currentThread().getSessionId(),
								Long.toString( message.requestId() ), OperationCallEvent.FAULT,
								"TypeMismatch:" + e.getMessage(), outputPort.id(), message.value(),
								Long.toString( message.getId() ) ) );
					}
					throw (e);
				}
			}

			CommChannel channel = outputPort.getCommChannel();
			channel.send( message );

			if( Interpreter.getInstance().isMonitoring() ) {
				Interpreter.getInstance()
					.fireMonitorEvent( new OperationCallEvent( operationId,
						ExecutionThread.currentThread().getSessionId(), Long.toString( message.requestId() ),
						OperationCallEvent.SUCCESS, "", outputPort.id(), message.value(),
						Long.toString( message.getId() ) ) );
			}

			CommMessage response = null;
			while( response == null ) {
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
			}

			// Check to see if it there is Fault in response

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
									Long.toString( response.getId() ) ) );
						}
					} catch( TypeCheckingException e ) {
						if( Interpreter.getInstance().isMonitoring() ) {
							Interpreter.getInstance()
								.fireMonitorEvent( new OperationReplyEvent( operationId,
									ExecutionThread.currentThread().getSessionId(),
									Long.toString( response.requestId() ), OperationReplyEvent.FAULT,
									"TypeMismatch on fault:" + response.fault().faultName() + "." + e.getMessage(),
									outputPort.id(), response.fault().value(), Long.toString( response.getId() ) ) );
						}
						throw (new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
							"Received fault " + response.fault().faultName() + " TypeMismatch (" + operationId + "@"
								+ outputPort.id() + "): " + e.getMessage() ).toRuntimeFaultException());
					}
				} else {
					if( Interpreter.getInstance().isMonitoring() ) {
						Interpreter.getInstance().fireMonitorEvent(
							new OperationReplyEvent( operationId, ExecutionThread.currentThread().getSessionId(),
								Long.toString( response.requestId() ), OperationReplyEvent.FAULT,
								response.fault().faultName(), outputPort.id(), response.fault().value(),
								Long.toString( response.getId() ) ) );
					}
				}
				throw response.fault().toRuntimeFaultException();
			}

			return response.value();
		} catch( IOException e ) {
			throw (new FaultException( Constants.TIMEOUT_EXCEPTION_FAULT_NAME )).toRuntimeFaultException();
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TimeoutException e ) {
			throw (new FaultException( Constants.TIMEOUT_EXCEPTION_FAULT_NAME )).toRuntimeFaultException();
		} catch( TypeCheckingException e ) {
			throw (new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
				"Output message TypeMismatch (" + operationId + "@" + outputPort.id() + ") " + e.getMessage() ))
					.toRuntimeFaultException();
		}
		return null;
	}
}
