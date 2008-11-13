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
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.OutputPort;
import jolie.runtime.Expression;
import jolie.util.LocationParser;

public class NotificationProcess implements Process
{
	final private String operationId;
	final private OutputPort outputPort;
	final private Expression outputExpression;

	public NotificationProcess(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression
			)
	{
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new NotificationProcess(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason )
				);
	}
	
	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		try {
			URI uri = new URI( outputPort.locationVariablePath().getValue().strValue() );
			CommMessage message =
				( outputExpression == null ) ?
						new CommMessage( operationId, LocationParser.getResourcePath( uri ) ) :
						new CommMessage( operationId, LocationParser.getResourcePath( uri ), outputExpression.evaluate() );

			CommChannel channel = outputPort.getCommChannel();
			channel.send( message );
			channel.close();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}