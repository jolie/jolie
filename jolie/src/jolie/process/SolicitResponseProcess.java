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

import jolie.deploy.OutputPort;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class SolicitResponseProcess implements Process
{
	private String operationId;
	private OutputPort outputPort;
	private VariablePath inputVarPath; // each may be null
	private Expression outputExpression;
	private Process installProcess;

	public SolicitResponseProcess(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression,
			VariablePath inputVarPath,
			Process installProcess
			)
	{
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new SolicitResponseProcess( operationId, outputPort, outputExpression, inputVarPath, installProcess );
	}
	
	public void run()
		throws FaultException
	{
		CommChannel channel = null;
		try {
			channel = outputPort.getCommChannel();
			CommMessage message =
				( outputExpression == null ) ?
						new CommMessage( operationId ) :
						new CommMessage( operationId, outputExpression.evaluate() );
			
			channel.send( message );
			message = channel.recv();
			
			if ( inputVarPath != null )	 {
				Value v = inputVarPath.getValue();
				v.erase();
				v.deepCopy( message.value() );
			}
			
			if ( message.isFault() )
				throw message.fault();
			
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