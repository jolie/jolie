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
import jolie.runtime.Expression;
import jolie.runtime.OutputOperation;
import jolie.runtime.Value;

public class NotificationProcess implements Process
{
	private OutputOperation operation;
	private Expression location, outputExpression;
	//private OperationChannelInfo channelInfo;

	public NotificationProcess(
			OutputOperation operation,
			Expression location,
			Expression outputExpression
		//	OperationChannelInfo channelInfo
			)
	{
		this.operation = operation;
		this.outputExpression = outputExpression;
		this.location = location;
		//this.channelInfo = channelInfo;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new NotificationProcess( operation, location, outputExpression );
	}
	
	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;

		try {
			CommMessage message =
				( outputExpression == null ) ?
						new CommMessage( operation.id() ) :
						new CommMessage( operation.id(), outputExpression.evaluate() );

			CommChannel channel;
			Value loc = location.evaluate();
			if ( loc.isChannel() )
				channel = loc.channelValue();
			else {
				URI uri = new URI( location.evaluate().strValue() );
				channel =
					CommChannel.createCommChannel(
						uri,
						operation.getOutputProtocol( uri )
						);
			}
			channel.send( message );
			channel.close();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		}
	}
}