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
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

/**
 * 
 * @author Fabrizio Montesi
 */
public class ForwardNotificationProcess implements Process {
	private final String operationName;
	private final OutputPort outputPort;
	private final VariablePath outputVariablePath;
	private final OneWayTypeDescription aggregatedTypeDescription, extenderTypeDescription;
	private final ParsingContext context;

	public ForwardNotificationProcess(
		String operationName,
		OutputPort outputPort,
		VariablePath outputVariablePath,
		OneWayTypeDescription aggregatedTypeDescription,
		OneWayTypeDescription extenderTypeDescription,
		ParsingContext context ) {
		this.operationName = operationName;
		this.outputPort = outputPort;
		this.outputVariablePath = outputVariablePath;
		this.aggregatedTypeDescription = aggregatedTypeDescription;
		this.extenderTypeDescription = extenderTypeDescription;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new ForwardNotificationProcess(
			operationName,
			outputPort,
			outputVariablePath,
			aggregatedTypeDescription,
			extenderTypeDescription,
			context );
	}

	private void log( String log, CommMessage message ) {
		Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.COURIER_NOTIFICATION,
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

			log( "SENT", message );

			CommMessage response = null;
			do {
				try {
					response = channel.recvResponseFor( message ).get();
				} catch( InterruptedException | ExecutionException e ) {
					Interpreter.getInstance().logFine( e );
				}
			} while( response == null );

			log( "RECEIVED ACK", response );

			if( response.isFault() ) {
				if( response.fault().faultName().equals( "CorrelationError" )
					|| response.fault().faultName().equals( "IOException" )
					|| response.fault().faultName().equals( "TypeMismatch" ) ) {
					throw response.fault();
				} else {
					Interpreter.getInstance().logSevere( "Forward notification process for operation " + operationName
						+ " received an unexpected fault: " + response.fault().faultName() );
				}
			}
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME,
				"TypeMismatch (" + operationName + "@" + outputPort.id() + "): " + e.getMessage() );
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
