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
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.Location;
import jolie.runtime.OutputOperation;

public class NotificationProcess implements Process
{
	private OutputOperation operation;
	private GlobalVariablePath varPath;
	private Location location;

	public NotificationProcess(
			OutputOperation operation,
			Location location,
			GlobalVariablePath varPath
			)
	{
		this.operation = operation;
		this.varPath = varPath;
		this.location = location;
	}
	
	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;

		try {
			CommMessage message =
				( varPath == null ) ?
						new CommMessage( operation.id() ) :
						new CommMessage( operation.id(), varPath.getValue() );
			CommChannel channel =
				CommChannel.createCommChannel(
						location,
						operation.getOutputProtocol( location )
						);
			channel.send( message );
			channel.close();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		}
	}
}