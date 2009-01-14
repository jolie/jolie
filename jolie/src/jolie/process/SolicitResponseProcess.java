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
import jolie.runtime.ExitingException;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.util.LocationParser;

public class SolicitResponseProcess implements Process
{
	final private String operationId;
	final private OutputPort outputPort;
	final private VariablePath inputVarPath; // may be null
	final private Expression outputExpression; // may be null
	final private Process installProcess; // may be null
	final private RequestResponseTypeDescription types;

	public SolicitResponseProcess(
			String operationId,
			OutputPort outputPort,
			Expression outputExpression,
			VariablePath inputVarPath,
			Process installProcess,
			RequestResponseTypeDescription types
	) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
		this.types = types;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new SolicitResponseProcess(
					operationId,
					outputPort,
					( outputExpression == null ) ? null : outputExpression.cloneExpression( reason ),
					( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
					( installProcess == null ) ? null : installProcess.clone( reason ),
					types
				);
	}
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}
		
		CommChannel channel = null;
		try {
			URI uri = outputPort.getLocation();
			CommMessage message =
				CommMessage.createRequest(
					operationId,
					LocationParser.getResourcePath( uri ),
					( outputExpression == null ) ? Value.create() : outputExpression.evaluate()
				);

			if ( types.requestType() != null ) {
				types.requestType().check( message.value() );
			}

			channel = outputPort.getCommChannel();
			channel.send( message );
			message = channel.recvResponseFor( message );
			
			if ( inputVarPath != null )	 {
				inputVarPath.getValue().refCopy( message.value() );
			}
			
			if ( message.isFault() ) {
				Type faultType = types.getFaultType( message.fault().faultName() );
				try {
					faultType.check( message.fault().value() );
				} catch( TypeCheckingException e ) {
					throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received fault TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
				}
				throw message.fault();
			} else {
				if ( types.responseType() != null ) {
					try {
						types.responseType().check( message.value() );
					} catch( TypeCheckingException e ) {
						throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Received message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
					}
				}
			}

			try {
				installProcess.run();
			} catch( ExitingException e ) { assert false; }
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		} catch( TypeCheckingException e ) {
			throw new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Output message TypeMismatch (" + operationId + "@" + outputPort.id() + "): " + e.getMessage() );
		} finally {
			if ( channel != null ) {
				try {
					channel.release();
				} catch( IOException ioe ) {
					ioe.printStackTrace();
				}
			}
		}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}