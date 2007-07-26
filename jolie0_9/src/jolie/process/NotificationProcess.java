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
import java.util.Vector;

import jolie.ExecutionThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.GlobalVariable;
import jolie.runtime.Location;
import jolie.runtime.OutputOperation;
import jolie.runtime.Variable;

public class NotificationProcess implements Process
{
	private OutputOperation operation;
	private Vector< GlobalVariable > varsVec;
	private Location location;

	public NotificationProcess( OutputOperation operation, Location location, Vector< GlobalVariable > varsVec )
	{
		this.operation = operation;
		this.varsVec = varsVec;
		this.location = location;
	}
	
	public void run()
	{
		if ( ExecutionThread.killed() )
			return;
		Variable.castAll( varsVec, operation.outVarTypes() );
		try {
			/*if ( wsdlInfo.outVarNames() == null ) {
				wsdlInfo = operation.wsdlInfo().clone();
				wsdlInfo.setOutVarNames( Variable.getNames( varsVec ) );
			}
			
			if ( wsdlInfo.boundName() == null )
				wsdlInfo.setBoundName( operation.boundOperationId() );*/
			String boundName = operation.wsdlInfo().boundName();
			if ( boundName == null )
				boundName = operation.boundOperationId();

			CommMessage message = new CommMessage( boundName, varsVec );
			CommChannel channel = new CommChannel( location, operation.getOutputProtocol( location ) );
			channel.send( message );
			channel.close();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		}
	}
}