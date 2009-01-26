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
import java.net.URI;
import java.net.URISyntaxException;

import jolie.ExecutionThread;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.OutputPort;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.util.LocationParser;

public class NotificationProcess implements Process
{
	final private String operationId;
	final private OutputPort outputPort;
	final private Expression outputExpression; // may be null
	final private Type outputType; // may be null

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
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		try {
			URI uri = outputPort.getLocation();
			CommMessage message =
				( outputExpression == null ) ?
						new CommMessage( operationId, LocationParser.getResourcePath( uri ) ) :
						new CommMessage( operationId, LocationParser.getResourcePath( uri ), outputExpression.evaluate() );

			if ( outputType != null ) {
				outputType.check( message.value() );
			}

			CommChannel channel = outputPort.getCommChannel();
			channel.send( message );
			channel.release();
		} catch( IOException e ) {
			throw new FaultException( "IOException", e );
		} catch( URISyntaxException e ) {
			e.printStackTrace();
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}