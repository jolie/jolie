/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.behaviours;

import java.io.IOException;
import java.util.ArrayList;
import jolie.Interpreter;
import jolie.StatefulContext;
import jolie.lang.Constants;
import jolie.monitoring.events.OperationEndedEvent;
import jolie.monitoring.events.OperationStartedEvent;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.SessionMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;
import jolie.util.Pair;

public class RequestResponseBehaviour implements InputOperationBehaviour
{
	
	private class RequestResponseExecutionBehaviour extends SimpleBehaviour {
		
		private final SessionMessage sessionMessage;
		private final ReThrowBehaviour reThrowBehaviour = new ReThrowBehaviour();
		private final String scopeId;

		public RequestResponseExecutionBehaviour( SessionMessage sessionMessage )
		{
			this.sessionMessage = sessionMessage;
			this.scopeId = Math.random() + sessionMessage.message().id() + "-RequestResponseScope";
		}
		
		@Override
		public void run( StatefulContext ctx ) throws FaultException, ExitingException
		{
			if ( ctx.interpreter().isMonitoring() && !isSessionStarter ) {
				ctx.interpreter().fireMonitorEvent( new OperationStartedEvent( ctx, operation.id(), Long.toString(sessionMessage.message().id()), sessionMessage.message().value() ) );
			}

			log( ctx.interpreter(), "RECEIVED", sessionMessage.message() );
			if ( inputVarPath != null ) {
				inputVarPath.getValue( ctx.state().root() ).refCopy( sessionMessage.message().value() );
			}

			// This scope id must not collide with user defined scope
			ArrayList<Pair<String, Behaviour>> faultHandlers = new ArrayList<>();
			faultHandlers.add( new Pair( Constants.Keywords.DEFAULT_HANDLER_NAME, new FaultBehaviour( sessionMessage.channel(), sessionMessage.message() ) ) );
			ctx.executeNext(
				new SequentialBehaviour(new Behaviour[] {
					new ScopeBehaviour(
						scopeId,
						new SequentialBehaviour(new Behaviour[] {
							new InstallBehaviour( faultHandlers ),
							process,
							new PostBehaviour( sessionMessage.channel(), sessionMessage.message() )
						}),
						true, false
					),
					reThrowBehaviour
				})
			);
		}
		
			
		private class PostBehaviour extends SimpleBehaviour {

			private final CommMessage message;
			private final CommChannel channel;

			public PostBehaviour(CommChannel channel, CommMessage message)
			{			
				this.channel = channel;
				this.message = message;
			}

			@Override
			public void run( StatefulContext ctx )
				throws FaultException, ExitingException
			{			
				// Variables for monitor
				int responseStatus;
				String details;

				FaultException typeMismatch = null;
				CommMessage response;
				if ( ctx.isKilled() ) {
					try {
						response = createFaultMessage( ctx.interpreter(), message, ctx.killerFault() );
						responseStatus = OperationEndedEvent.FAULT;
						details = ctx.killerFault().faultName();
					} catch( TypeCheckingException e ) {
						typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + ctx.killerFault().faultName() + " (operation " + operation.id() + "): " + e.getMessage() );
						response = CommMessage.createFaultResponse( message, typeMismatch );
						responseStatus = OperationEndedEvent.ERROR;
						details = typeMismatch.faultName();
					}
				} else {
					response =
						CommMessage.createResponse(
							message,
							( outputExpression == null ) ? Value.UNDEFINED_VALUE : outputExpression.evaluate()
						);
						responseStatus = OperationEndedEvent.SUCCESS;
						details = "";
					if ( operation.typeDescription().responseType() != null ) {
						try {
							operation.typeDescription().responseType().check( response.value() );
						} catch( TypeCheckingException e ) {						
							typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response input operation output value TypeMismatch (operation " + operation.id() + "): " + e.getMessage() );						
							response = CommMessage.createFaultResponse( message, new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error (TypeMismatch)" ) );
							responseStatus = OperationEndedEvent.ERROR;
							details =  Constants.TYPE_MISMATCH_FAULT_NAME;
						}
					}
				}
				ctx.executeNext( new FinallyBehaviour(response, details, responseStatus, typeMismatch, channel, null ));
			}
		}

		private class FaultBehaviour extends SimpleBehaviour {

			private final CommChannel channel;
			private final CommMessage message;

			public FaultBehaviour(CommChannel channel, CommMessage message)
			{
				this.channel = channel;
				this.message = message;
			}

			@Override
			public Behaviour clone( TransformationReason reason )
			{
				return new FaultBehaviour( channel, message);
			}

			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				// Variables for monitor
				int responseStatus;
				String details;

				FaultException typeMismatch = null;
				CommMessage response;

				Value scopeValue = new VariablePathBuilder( false ).add( scopeId, 0 ).toVariablePath().getValue( ctx );
				Value defaultFaultValue = scopeValue.getChildren( Constants.Keywords.DEFAULT_HANDLER_NAME ).get( 0 );
				Value userFaultValueValue = scopeValue.getChildren( defaultFaultValue.strValue() ).get( 0 );
				FaultException fault = new FaultException( defaultFaultValue.strValue(), userFaultValueValue );
				
				try {
					response = createFaultMessage( ctx.interpreter(), message, fault );
					responseStatus = OperationEndedEvent.FAULT;
					details = fault.faultName();
				} catch( TypeCheckingException e ) {
					typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + fault.faultName() + " (operation " + operation.id() + "): " + e.getMessage() );				
					response = CommMessage.createFaultResponse( message, typeMismatch );
					responseStatus = OperationEndedEvent.ERROR;
					details = typeMismatch.faultName();
				}
				ctx.executeNext( new FinallyBehaviour(response, details, responseStatus, typeMismatch, channel, fault ) );
			}

		}

		private class FinallyBehaviour extends SimpleBehaviour {

			private final CommMessage response;
			private final String details;
			private final int responseStatus;
			private final FaultException typeMismatch;
			private final CommChannel channel;
			private final FaultException fault;

			public FinallyBehaviour( CommMessage response, String details, int responseStatus, FaultException typeMismatch, CommChannel channel, FaultException fault)
			{
				this.response = response;
				this.details = details;
				this.responseStatus = responseStatus;
				this.typeMismatch = typeMismatch;
				this.channel = channel;
				this.fault = fault;
			}

			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				if (ctx.isKilled())
					return;
				
				try {
					final CommMessage msg = response;
					final String fDetails = details;
					final int rStatus = responseStatus;
					channel.send( ctx, response, ( Void ) -> {
						Value monitorValue;
						if ( msg.isFault() ) {
							log( ctx.interpreter(), "SENT FAULT", msg );					
							monitorValue = msg.fault().value();
						} else {
							log( ctx.interpreter(), "SENT", msg );
							monitorValue = msg.value();
						}
						if ( ctx.interpreter().isMonitoring() ) {
							ctx.interpreter().fireMonitorEvent(
								new OperationEndedEvent( ctx, operation.id(), Long.toString( msg.id() ), rStatus, fDetails, monitorValue ));
						}
						
						try {
							channel.release(); // TODO: what if the channel is in disposeForInput?
						} catch( IOException e ) {
							ctx.interpreter().logSevere( e );
						}
					
						return null;
					});

				} catch( IOException e ) {
					//Interpreter.getInstance().logSevere( e );
					throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
				}  finally {
				}

				if ( fault != null ) {
					if ( typeMismatch != null ) {
						ctx.interpreter().logWarning( typeMismatch.value().strValue() );
					}
					reThrowBehaviour.setFault( fault );
				} else if ( typeMismatch != null ) {
					reThrowBehaviour.setFault( typeMismatch );
				}
			}

		}

		private class ReThrowBehaviour extends SimpleBehaviour {

			private FaultException fault = null;

			@Override
			public void run( StatefulContext ctx ) throws FaultException, ExitingException
			{
				if (fault != null)
					throw fault;
			}

			protected void setFault(FaultException fault) {
				this.fault = fault;
			}
		}
	}
	
	private final RequestResponseOperation operation;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Behaviour process;
	private boolean isSessionStarter = false;
	
	public RequestResponseBehaviour(
			RequestResponseOperation operation,
			VariablePath inputVarPath,
			Expression outputExpression,
			Behaviour process )
	{
		this.operation = operation;
		this.inputVarPath = inputVarPath;
		this.process = process;
		this.outputExpression = outputExpression;
	}
	
	@Override
	public void setSessionStarter( boolean isSessionStarter )
	{
		this.isSessionStarter = isSessionStarter;
	}

	@Override
	public InputOperation inputOperation()
	{
		return operation;
	}

	private void log( Interpreter interpreter, String log, CommMessage message )
	{
		final Tracer tracer = interpreter.tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.REQUEST_RESPONSE,
			operation.id(),
			log,
			message
		) );
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}

	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return new RequestResponseBehaviour(
			operation,
			( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
			( outputExpression == null ) ? null : (VariablePath)outputExpression.cloneExpression( reason ),
			process.clone( reason )
		);
	}
	
	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		SessionMessage message = ctx.requestMessage( operation, ctx );
		if ( message == null) {
			ctx.executeNext( this );
			ctx.pauseExecution();
			return;
		}
		
		ctx.executeNext( receiveMessage( message, ctx ) );
	}
	
	public VariablePath inputVarPath()
	{
		return inputVarPath;
	}

	private CommMessage createFaultMessage( Interpreter interpreter, CommMessage request, FaultException f )
		throws TypeCheckingException
	{
		if ( operation.typeDescription().faults().containsKey( f.faultName() ) ) {
			Type faultType = operation.typeDescription().faults().get( f.faultName() );
			if ( faultType != null ) {
				faultType.check( f.value() );
			}
		} else {			
			interpreter.logSevere(
				"Request-Response process for " + operation.id() +
				" threw an undeclared fault for that operation (" + f.faultName() + "), throwing TypeMismatch" );
			f = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error" );
		}
		return CommMessage.createFaultResponse( request, f );
	}
	
	@Override
	public Behaviour receiveMessage( SessionMessage sessionMessage, StatefulContext ctx )
	{
		return new RequestResponseExecutionBehaviour( sessionMessage );
	}
	
}
