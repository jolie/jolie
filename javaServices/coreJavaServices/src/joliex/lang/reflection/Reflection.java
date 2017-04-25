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
import java.util.ArrayList;
import jolie.Interpreter;
import jolie.SessionListener;
import jolie.StatefulContext;
import jolie.TransparentContext;
import jolie.behaviours.Behaviour;
import jolie.behaviours.InstallBehaviour;
import jolie.behaviours.NotificationBehaviour;
import jolie.behaviours.NullBehaviour;
import jolie.behaviours.ScopeBehaviour;
import jolie.behaviours.SequentialBehaviour;
import jolie.behaviours.SolicitResponseBehaviour;
import jolie.behaviours.TransformationReason;
import jolie.behaviours.UnkillableBehaviour;
import jolie.lang.Constants;
import jolie.net.ports.OutputPort;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.util.Pair;

public class Reflection extends JavaService
{
	private class FaultBehaviour implements UnkillableBehaviour {
		
		private final String scopeId;
		private final FaultReference faultReference;

		public FaultBehaviour( String scopeId, FaultReference faultReference )
		{
			this.scopeId = scopeId;
			this.faultReference = faultReference;
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			
			Value scopeValue = new VariablePathBuilder( false ).add( scopeId, 0 ).toVariablePath().getValue( ctx );
			Value defaultFaultValue = scopeValue.getChildren( Constants.Keywords.DEFAULT_HANDLER_NAME ).get( 0 );
			Value userFaultValueValue = scopeValue.getChildren( defaultFaultValue.strValue() ).get( 0 );
			FaultException fault = new FaultException( defaultFaultValue.strValue(), userFaultValueValue );
			faultReference.fault = fault;
		}

		@Override
		public Behaviour clone( TransformationReason reason )
		{
			return this;
		}
		
	}
	
	private class FinallyBehaviour implements UnkillableBehaviour {
		
		private final FaultReference faultReference;

		public FinallyBehaviour( FaultReference faultReference )
		{
			this.faultReference = faultReference;
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{

			if ( faultReference.fault != null ) {
				throw faultReference.fault;
			}
			context().start();
		}
		
	}
	
	private static class FaultReference {
		private FaultException fault = null;
	}
	
	private Interpreter interpreter;

	public Reflection()
	{
		//this.interpreter = super.context().interpreter();
	}
	
	private Value runSolicitResponseInvocation( String operationName, OutputPort port, Value data, RequestResponseTypeDescription desc )
		throws FaultException, InterruptedException
	{
		Value ret = Value.create();
		Behaviour b = new SolicitResponseBehaviour(
			operationName,
			port,
			data,
			new ClosedVariablePath( new Pair[0], ret ),
			NullBehaviour.getInstance(),
			desc
		);
		
		final FaultReference ref = new FaultReference();
		
		// This scope id must not collide with user defined scope
		String scopeId = b.hashCode() + "-" + operationName + "-ReflectionScope";
		ArrayList<Pair<String, Behaviour>> faultHandlers = new ArrayList<>();
		faultHandlers.add( new Pair( Constants.Keywords.DEFAULT_HANDLER_NAME, new FaultBehaviour( scopeId, ref ) ) );
		Behaviour scopedBehaviour = new SequentialBehaviour(new Behaviour[] {
			new ScopeBehaviour(
				scopeId,
				new SequentialBehaviour(new Behaviour[] {
					new InstallBehaviour( faultHandlers ),
					b
				}),
				true, true
			)
		});
		
//		context().executeNext( new FinallyBehaviour( ref ));
		StatefulContext tranparentContext = new TransparentContext(scopedBehaviour, context() ) {};
		tranparentContext.start();
		tranparentContext.join();
		if ( ref.fault != null ) {
			throw ref.fault;
		}
		
		return ret;
	}
	
	private Value runNotificationInvocation( String operationName, OutputPort port, Value data, OneWayTypeDescription desc )
		throws FaultException, InterruptedException
	{
		Value ret = Value.create();
		Behaviour b = new NotificationBehaviour (
			operationName,
			port,
			data,
			desc
		);
		
		StatefulContext ctx = new StatefulContext( b, interpreter.initContext() );
		final FaultReference ref = new FaultReference();
		ctx.addSessionListener( new SessionListener() {
			@Override
			public void onSessionExecuted( StatefulContext session )
			{}

			@Override
			public void onSessionError( StatefulContext session, FaultException fault )
			{
				ref.fault = fault;
			}
		} );
		ctx.start();
		ctx.join();
		if ( ref.fault != null ) {
			throw ref.fault;
		}
		return ret;
	}

	@RequestResponse
	public Value invoke( Value request )
		throws FaultException
	{
		final String operation = request.getFirstChild( "operation" ).strValue();
		final String outputPortName = request.getFirstChild( "outputPort" ).strValue();
		final String resourcePath = ( request.hasChildren( "resourcePath" ) ) ? request.getFirstChild( "resourcePath" ).strValue() : "/";
		final Value data = request.getFirstChild( "data" );
		try {
			OutputPort port = interpreter.getOutputPort( request.getFirstChild( "outputPort").strValue() );
			OperationTypeDescription opDesc = port.getOperationTypeDescription( operation, resourcePath );
			if ( opDesc == null ) {
				throw new InvalidIdException( operation );
			} else if ( opDesc instanceof RequestResponseTypeDescription ) {
				return runSolicitResponseInvocation( operation, port, data, opDesc.asRequestResponseTypeDescription() );
			} else if ( opDesc instanceof OneWayTypeDescription ) {
				return runNotificationInvocation( operation, port, data, opDesc.asOneWayTypeDescription() );
			}
			throw new InvalidIdException( operation );
		} catch( InvalidIdException e ) {
			throw new FaultException( "OperationNotFound", "Could not find operation " + operation + "@" + outputPortName );
		} catch( InterruptedException e ) {
			interpreter.logSevere( e );
			throw new FaultException( new IOException( "Interrupted" ) );
		} catch( FaultException e ) {
			Value v = Value.create();
			v.setFirstChild( "name", e.faultName() );
			v.getChildren( "data" ).set( 0, e.value() );
			throw new FaultException( "InvocationFault", v );
		}
	}
	
	@Override
	public void setContext( StatefulContext context )
	{
		super.setContext( context );
		interpreter = context.interpreter();
	}
}
