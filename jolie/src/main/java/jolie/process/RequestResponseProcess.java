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
import java.util.concurrent.Future;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;
import jolie.monitoring.events.OperationEndedEvent;
import jolie.monitoring.events.OperationStartedEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class RequestResponseProcess implements InputOperationProcess {
	private final RequestResponseOperation operation;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Process process;
	private final ParsingContext context;

	public RequestResponseProcess(
		RequestResponseOperation operation,
		VariablePath inputVarPath,
		Expression outputExpression,
		Process process,
		ParsingContext context ) {
		this.operation = operation;
		this.inputVarPath = inputVarPath;
		this.process = process;
		this.outputExpression = outputExpression;
		this.context = context;
	}

	public InputOperation inputOperation() {
		return operation;
	}

	private void log( String log, CommMessage message ) {
		final Tracer tracer = Interpreter.getInstance().tracer();

		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.REQUEST_RESPONSE,
			operation.id(),
			log,
			message,
			context ) );
	}

	public boolean isKillable() {
		return true;
	}

	public Process copy( TransformationReason reason ) {
		return new RequestResponseProcess(
			operation,
			(inputVarPath == null) ? null : (VariablePath) inputVarPath.cloneExpression( reason ),
			(outputExpression == null) ? null : (VariablePath) outputExpression.cloneExpression( reason ),
			process.copy( reason ),
			context );
	}

	public Process receiveMessage( final SessionMessage sessionMessage, jolie.State state ) {

		log( "RECEIVED", sessionMessage.message() );
		if( inputVarPath != null ) {
			inputVarPath.getValue( state.root() ).refCopy( sessionMessage.message().value() );
		}

		return new Process() {
			public void run()
				throws FaultException, ExitingException {
				final String processId = ExecutionThread.currentThread().getSessionId();
				final String scopeId = ExecutionThread.currentThread().currentScopeId();
				Interpreter.getInstance().fireMonitorEvent( () -> {
					return new OperationStartedEvent( operation.id(), processId,
						Long.toString( sessionMessage.message().id() ),
						Interpreter.getInstance().programFilename(), scopeId, context,
						sessionMessage.message().value() );
				} );
				runBehaviour( sessionMessage.channel(), sessionMessage.message(), scopeId, processId );
			}

			public Process copy( TransformationReason reason ) {
				return this;
			}

			public boolean isKillable() {
				return false;
			}
		};
	}

	public void run()
		throws FaultException, ExitingException {
		ExecutionThread ethread = ExecutionThread.currentThread();
		if( ethread.isKilled() ) {
			return;
		}

		Future< SessionMessage > f = ethread.requestMessage( operation, ethread );
		try {
			try {
				SessionMessage m = f.get();
				if( m != null ) { // If it is null, we got killed by a fault
					receiveMessage( m, ethread.state() ).run();
				}
			} catch( FaultException.RuntimeFaultException rf ) {
				throw rf.faultException();
			}
		} catch( FaultException e ) {
			throw e;
		} catch( ExitingException e ) {
			throw e;
		} catch( Exception e ) {
			Interpreter.getInstance().logSevere( e );
		}
	}

	public VariablePath inputVarPath() {
		return inputVarPath;
	}

	private CommMessage createFaultMessage( CommMessage request, FaultException f )
		throws TypeCheckingException {
		if( operation.typeDescription().faults().containsKey( f.faultName() ) ) {
			Type faultType = operation.typeDescription().faults().get( f.faultName() );
			if( faultType != null ) {
				faultType.check( f.value() );
			}
		} else {
			Interpreter.getInstance().logSevere(
				"Request-Response process for " + operation.id() +
					" threw an undeclared fault for that operation (" + f.faultName() + "), throwing TypeMismatch" );
			f = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error" );
		}
		return CommMessage.createFaultResponse( request, f );
	}

	private void runBehaviour( CommChannel channel, CommMessage message, String scopeId, String processId )
		throws FaultException {
		// Variables for monitor
		int responseStatus;
		final StringBuilder details = new StringBuilder();

		FaultException typeMismatch = null;
		FaultException fault = null;
		CommMessage response;
		try {
			try {
				try {
					process.run();
				} catch( ExitingException e ) {
				}
				ExecutionThread ethread = ExecutionThread.currentThread();
				if( ethread.isKilled() ) {
					try {
						response = createFaultMessage( message, ethread.killerFault() );
						responseStatus = OperationEndedEvent.FAULT;
						details.append( ethread.killerFault().faultName() );
					} catch( TypeCheckingException e ) {
						typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
							"Request-Response process TypeMismatch for fault " + ethread.killerFault().faultName()
								+ " (operation " + operation.id() + "): " + e.getMessage() );
						response = CommMessage.createFaultResponse( message, typeMismatch );
						responseStatus = OperationEndedEvent.ERROR;
						details.append( typeMismatch.faultName() );
					}
				} else {
					response =
						CommMessage.createResponse(
							message,
							(outputExpression == null) ? Value.UNDEFINED_VALUE : outputExpression.evaluate() );
					responseStatus = OperationEndedEvent.SUCCESS;
					if( operation.typeDescription().responseType() != null ) {
						try {
							operation.typeDescription().responseType().check( response.value() );
						} catch( TypeCheckingException e ) {
							log( "TYPE MISMATCH", response );
							typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
								"Request-Response input operation output value TypeMismatch (operation "
									+ operation.id() + "): " + e.getMessage() );
							response = CommMessage.createFaultResponse( message, new FaultException(
								Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error (TypeMismatch)" ) );
							responseStatus = OperationEndedEvent.ERROR;
							details.append( Constants.TYPE_MISMATCH_FAULT_NAME );
						}
					}
				}
			} catch( FaultException.RuntimeFaultException rf ) {
				throw rf.faultException();
			}
		} catch( FaultException f ) {
			try {
				response = createFaultMessage( message, f );
				responseStatus = OperationEndedEvent.FAULT;
				details.append( f.faultName() );
			} catch( TypeCheckingException e ) {
				typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
					"Request-Response process TypeMismatch for fault " + f.faultName() + " (operation " + operation.id()
						+ "): " + e.getMessage() );
				response = CommMessage.createFaultResponse( message, typeMismatch );
				responseStatus = OperationEndedEvent.ERROR;
				details.append( typeMismatch.faultName() );
			}
			fault = f;
		}

		try {
			channel.send( response );
			final Value monitorValue;
			if( response.isFault() ) {
				log( "SENT FAULT", response );
				monitorValue = response.fault().value();
			} else {
				log( "SENT", response );
				monitorValue = response.value();
			}
			final long responseId = response.id();
			final int responseStatusforMonitor = responseStatus;


			Interpreter.getInstance().fireMonitorEvent( () -> {
				return new OperationEndedEvent( operation.id(), processId,
					Long.toString( responseId ), responseStatusforMonitor, details.toString(), monitorValue,
					Interpreter.getInstance().programFilename(), scopeId, context );
			} );

		} catch( IOException e ) {
			// Interpreter.getInstance().logSevere( e );
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} finally {
			try {
				channel.release(); // TODO: what if the channel is in disposeForInput?
			} catch( IOException e ) {
				Interpreter.getInstance().logSevere( e );
			}
		}

		if( fault != null ) {
			if( typeMismatch != null ) {
				Interpreter.getInstance().logWarning( typeMismatch.value().strValue() );
			}
			throw fault;
		} else if( typeMismatch != null ) {
			throw typeMismatch;
		}
	}
}
