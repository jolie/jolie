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

import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.SolicitResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class SolicitResponseProcess implements Process
{
	private SolicitResponseOperation operation;
	private VariablePath inputVarPath; // each may be null
	private Expression location, outputExpression;
	private Process installProcess;

	public SolicitResponseProcess(
			SolicitResponseOperation operation,
			Expression location,
			Expression outputExpression,
			VariablePath inputVarPath,
			Process installProcess
			)
	{
		this.operation = operation;
		this.location = location;
		this.outputExpression = outputExpression;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new SolicitResponseProcess( operation, location, outputExpression, inputVarPath, installProcess );
	}
	
	public void run()
		throws FaultException
	{
		CommChannel channel = null;
		try {
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

			CommMessage message =
				( outputExpression == null ) ?
						new CommMessage( operation.id() ) :
						new CommMessage( operation.id(), outputExpression.evaluate() );
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