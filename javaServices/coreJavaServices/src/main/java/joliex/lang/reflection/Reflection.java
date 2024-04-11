/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.lang.reflection;

import java.io.IOException;
import java.util.HashMap;
import jolie.ExecutionThread;
import jolie.SessionListener;
import jolie.SessionThread;
import jolie.TransparentExecutionThread;
import jolie.net.ports.OutputPort;
import jolie.process.NotificationProcess;
import jolie.process.NullProcess;
import jolie.process.SolicitResponseProcess;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.util.Pair;

public class Reflection extends JavaService {
	private static class FaultReference {
		private FaultException fault = null;
	}

	private Value runSolicitResponseInvocation( String operationName, OutputPort port, Value data,
		RequestResponseTypeDescription desc )
		throws FaultException, InterruptedException {
		Value ret = Value.create();
		@SuppressWarnings( "unchecked" )
		jolie.process.Process p = new SolicitResponseProcess(
			operationName,
			port,
			data,
			new ClosedVariablePath( new Pair[ 0 ], ret ),
			NullProcess.getInstance(),
			desc,
			null );
		final FaultReference ref = new FaultReference();
		ExecutionThread t = new TransparentExecutionThread( p, ExecutionThread.currentThread() ) {
			@Override
			public void runProcess() {
				try {
					process().run();
				} catch( FaultException f ) {
					ref.fault = f;
				} catch( ExitingException e ) {
				}
			}
		};
		t.start();
		t.join();
		if( ref.fault != null ) {
			throw ref.fault;
		}
		return ret;
	}

	private Value runNotificationInvocation( String operationName, OutputPort port, Value data,
		OneWayTypeDescription desc )
		throws FaultException, InterruptedException {
		Value ret = Value.create();
		jolie.process.Process p = new NotificationProcess(
			operationName,
			port,
			data,
			desc,
			null );
		SessionThread t = new SessionThread( p, interpreter().initThread() );
		final FaultReference ref = new FaultReference();
		t.addSessionListener( new SessionListener() {
			@Override
			public void onSessionExecuted( SessionThread session ) {}

			@Override
			public void onSessionError( SessionThread session, FaultException fault ) {
				ref.fault = fault;
			}
		} );
		t.start();
		t.join();
		if( ref.fault != null ) {
			throw ref.fault;
		}
		return ret;
	}

	@RequestResponse
	public Value invoke( Value request )
		throws FaultException {
		final String operation = request.getFirstChild( "operation" ).strValue();
		final String outputPortName = request.getFirstChild( "outputPort" ).strValue();
		final String resourcePath =
			(request.hasChildren( "resourcePath" )) ? request.getFirstChild( "resourcePath" ).strValue() : "/";
		final Value data = request.getFirstChild( "data" );
		try {
			OutputPort port = interpreter().getOutputPort( request.getFirstChild( "outputPort" ).strValue() );
			OperationTypeDescription opDesc = port.getOperationTypeDescription( operation, resourcePath );
			if( opDesc == null ) {
				throw new InvalidIdException( operation );
			} else if( opDesc instanceof RequestResponseTypeDescription ) {
				return runSolicitResponseInvocation( operation, port, data, opDesc.asRequestResponseTypeDescription() );
			} else if( opDesc instanceof OneWayTypeDescription ) {
				return runNotificationInvocation( operation, port, data, opDesc.asOneWayTypeDescription() );
			}
			throw new InvalidIdException( operation );
		} catch( InvalidIdException e ) {
			throw new FaultException( "OperationNotFound",
				"Could not find operation " + operation + "@" + outputPortName );
		} catch( InterruptedException e ) {
			interpreter().logSevere( e );
			throw new FaultException( new IOException( "Interrupted" ) );
		} catch( FaultException e ) {
			Value v = Value.create();
			v.setFirstChild( "name", e.faultName() );
			v.getChildren( "data" ).set( 0, e.value() );
			throw new FaultException( "InvocationFault", v );
		}
	}

	@RequestResponse
	public Value invokeRRUnsafe( Value request )
		throws FaultException {
		final String operation = request.getFirstChild( "operation" ).strValue();
		final String outputPortName = request.getFirstChild( "outputPort" ).strValue();
		// final String resourcePath =
		// (request.hasChildren( "resourcePath" )) ? request.getFirstChild( "resourcePath" ).strValue() :
		// "/";
		final Value data = request.getFirstChild( "data" );
		try {
			OutputPort port = interpreter().getOutputPort( request.getFirstChild( "outputPort" ).strValue() );
			return runSolicitResponseInvocation( operation, port, data,
				new RequestResponseTypeDescription( null, null, new HashMap<>() ) );
		} catch( InvalidIdException e ) {
			throw new FaultException( "OperationNotFound",
				"Could not find operation " + operation + "@" + outputPortName );
		} catch( InterruptedException e ) {
			interpreter().logSevere( e );
			throw new FaultException( new IOException( "Interrupted" ) );
		} catch( FaultException e ) {
			Value v = Value.create();
			v.setFirstChild( "name", e.faultName() );
			v.getChildren( "data" ).set( 0, e.value() );
			throw new FaultException( "InvocationFault", v );
		}
	}
}
