/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.process.courier;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.process.Process;
import jolie.process.TransformationReason;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

/**
 *
 * @author Fabrizio Montesi
 */
public class ForwardSolicitResponseProcess implements Process {
	private final String operationName;
	private final OutputPort outputPort;
	private final VariablePath outputVariablePath, inputVariablePath;
	private final RequestResponseTypeDescription aggregatedTypeDescription, extenderTypeDescription;
	private final ParsingContext context;

	public ForwardSolicitResponseProcess(
		String operationName,
		OutputPort outputPort,
		VariablePath outputVariablePath,
		VariablePath inputVariablePath,
		RequestResponseTypeDescription aggregatedTypeDescription,
		RequestResponseTypeDescription extenderTypeDescription,
		ParsingContext context ) {
		this.operationName = operationName;
		this.outputPort = outputPort;
		this.outputVariablePath = outputVariablePath;
		this.inputVariablePath = inputVariablePath;
		this.aggregatedTypeDescription = aggregatedTypeDescription;
		this.extenderTypeDescription = extenderTypeDescription;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new ForwardSolicitResponseProcess(
			operationName,
			outputPort,
			outputVariablePath,
			inputVariablePath,
			aggregatedTypeDescription,
			extenderTypeDescription,
			context );
	}

	private void log( String log, CommMessage message ) {
		Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.COURIER_SOLICIT_RESPONSE,
			operationName + "@" + outputPort.id(),
			log,
			message,
			context ) );
	}

	@Override
	public void run()
		throws FaultException {
		if( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		CommChannel channel = null;
		try {
			Value messageValue = outputVariablePath.evaluate();
			if( extenderTypeDescription != null ) {
				extenderTypeDescription.requestType().cutChildrenFromValue( messageValue );
			}
			aggregatedTypeDescription.requestType().check( messageValue );
			CommMessage message =
				CommMessage.createRequest( operationName, outputPort.getResourcePath(), messageValue );

			channel = outputPort.getCommChannel();

			log( "SENDING", message );

			channel.send( message );
			// channel.release(); TODO release channel if possible (i.e. it will not be closed)
			log( "SENT", message );
			CommMessage response = null;
			do {
				try {
					response = channel.recvResponseFor( message ).get();
				} catch( InterruptedException | ExecutionException e ) {
					Interpreter.getInstance().logFine( e );
				}
			} while( response == null );
			log( "RECEIVED", message );

			if( inputVariablePath != null ) {
				inputVariablePath.setValue( response.value() );
			}

			if( response.isFault() ) {
				Type faultType = aggregatedTypeDescription.getFaultType( response.fault().faultName() );
				if( faultType != null ) {
					try {
						faultType.check( response.fault().value() );
					} catch( TypeCheckingException e ) {
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
							"Received fault " + response.fault().faultName() + " TypeMismatch (" + operationName + "@"
								+ outputPort.id() + "): " + e.getMessage() )
									.withContext( this.context );
					}
				}
				throw response.fault().withContext( this.context );
			} else {
				if( aggregatedTypeDescription.responseType() != null ) {
					try {
						aggregatedTypeDescription.responseType().check( response.value() );
					} catch( TypeCheckingException e ) {
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received message TypeMismatch ("
							+ operationName + "@" + outputPort.id() + "): " + e.getMessage() )
								.withContext( this.context );
					}
				}
			}

			/*
			 * try { installProcess.run(); } catch( ExitingException e ) { assert false; }
			 */
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e )
				.withContext( this.context );
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
				"Output message TypeMismatch (" + operationName + "@" + outputPort.id() + "): " + e.getMessage() )
					.withContext( this.context );
		} finally {
			if( channel != null ) {
				try {
					channel.release();
				} catch( IOException e ) {
					Interpreter.getInstance().logWarning( e );
				}
			}
		}
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
