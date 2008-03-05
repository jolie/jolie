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
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Expression;
import jolie.runtime.OutputOperation;

public class NotificationProcess implements Process
{
	private OutputOperation operation;
	private Expression outputExpression;

	public NotificationProcess(
			OutputOperation operation,
			Expression outputExpression
			)
	{
		this.operation = operation;
		this.outputExpression = outputExpression;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new NotificationProcess( operation, outputExpression );
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

			CommChannel channel = operation.outputPort().createCommChannel();
			channel.send( message );
			channel.close();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		}
	}
}