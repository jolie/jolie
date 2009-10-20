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
import jolie.net.OutputPort;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class NotificationProcess implements Process
{
	private final String operationId;
	private final OutputPort outputPort;
	private final Expression outputExpression; // may be null
	private final Type outputType; // may be null

	public NotificationProcess(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression,
			Type outputType
			)
	{
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.outputType = outputType;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new NotificationProcess(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason ),
					outputType
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

		try {
			CommMessage message =
				( outputExpression == null ) ?
						CommMessage.createOneWayMessage( operationId, outputPort.getResourcePath(), Value.UNDEFINED_VALUE ) :
						CommMessage.createOneWayMessage( operationId, outputPort.getResourcePath(), outputExpression.evaluate() );
			if ( outputType != null ) {
				outputType.check( message.value() );
			}
			CommChannel channel = outputPort.getCommChannel();
			if ( verbose ) {
				log( "sending request " + message.id() );
			}
			channel.send( message );
			if ( verbose ) {
				log( "request " + message.id() + " sent" );
			}
			channel.release();
		} catch( IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} catch( URISyntaxException e ) {
			Interpreter.getInstance().logSevere( e );
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}