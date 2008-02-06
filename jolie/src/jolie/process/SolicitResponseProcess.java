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

import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.VariablePath;
import jolie.runtime.Location;
import jolie.runtime.SolicitResponseOperation;

public class SolicitResponseProcess implements Process
{
	private SolicitResponseOperation operation;
	private VariablePath outputVarPath, inputVarPath; // each may be null
	private Location location;
	private Process installProcess;

	public SolicitResponseProcess(
			SolicitResponseOperation operation,
			Location location,
			VariablePath outputVarPath,
			VariablePath inputVarPath,
			Process installProcess
			)
	{
		this.operation = operation;
		this.location = location;
		this.outputVarPath = outputVarPath;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new SolicitResponseProcess( operation, location, outputVarPath, inputVarPath, installProcess );
	}
	
	public void run()
		throws FaultException
	{
		CommChannel channel = null;
		try {
			channel =
				CommChannel.createCommChannel(
					location,
					operation.getOutputProtocol( location )
					);

			CommMessage message =
				( outputVarPath == null ) ?
						new CommMessage( operation.id() ) :
						new CommMessage( operation.id(), outputVarPath.getValue() );
			channel.send( message );

			message = channel.recv();
			if ( message.isFault() )
				throw new FaultException( message.faultName() );

			if ( inputVarPath != null )				
				inputVarPath.getValue().deepCopy( message.value() );
			
			installProcess.run();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		} finally {
			if ( channel != null ) {
				try {
					channel.close();
				} catch( IOException ioe ) {
					ioe.printStackTrace();
				}
			}
		}
	}
}