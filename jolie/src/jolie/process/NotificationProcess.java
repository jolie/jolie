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

package jolie.process;

import java.io.IOException;
import java.net.URISyntaxException;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.ports.OutputPort;
import jolie.runtime.expression.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.TypeCheckingException;

public class NotificationProcess implements Process
{
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression; // may be null
	private final OneWayTypeDescription oneWayDescription; // may be null

	public NotificationProcess(
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
	
	public Process clone( TransformationReason reason )
	{
		return new NotificationProcess(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason ),
					oneWayDescription
				);
	}

	private void log( String message )
	{
		Interpreter.getInstance().logInfo( "[Notification operation " + operationId + "@" + outputPort.id() + "]: " + message );
	}

	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		boolean verbose = Interpreter.getInstance().verbose();
		CommChannel channel = null;
		try {
			CommMessage message =
				( outputExpression == null ) ?
						CommMessage.createRequest( operationId, outputPort.getResourcePath(), Value.UNDEFINED_VALUE ) :
						CommMessage.createRequest( operationId, outputPort.getResourcePath(), outputExpression.evaluate() );
			if ( oneWayDescription != null ) {
				oneWayDescription.requestType().check( message.value() );
			}
			channel = outputPort.getCommChannel();

			if ( verbose ) {
				log( "sending request " + message.id() );
			}
			channel.send( message );
			if ( verbose ) {
				log( "request " + message.id() + " sent" );
			}
			CommMessage response = null;
			do {
				response = channel.recvResponseFor( message );
			} while( response == null );
			if ( verbose ) {
				log( "received response for request " + response.id() );
			}
			if ( response.isFault() ) {
				if ( response.fault().faultName().equals( "CorrelationError" )
					|| response.fault().faultName().equals( "IOException" )
					|| response.fault().faultName().equals( "TypeMismatch" )
					) {
					throw response.fault();
				} else {
					Interpreter.getInstance().logSevere( "Notification process for operation " + operationId + " received an unexpected fault: " + response.fault().faultName() );
				}
			}
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
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