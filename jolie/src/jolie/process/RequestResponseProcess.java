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

package jolie.process;

import java.io.IOException;
import java.util.concurrent.Future;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.SessionContext;
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
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class RequestResponseProcess implements InputOperationProcess
{
	private final Process ListenProcess = new SimpleProcess()
	{

		@Override
		public void run( SessionContext ctx ) throws FaultException, ExitingException
		{

		}
	};
	
	private class RequestResponseListenProcess implements Process {
		
		private final Future< SessionMessage > future;

		public RequestResponseListenProcess( Future<SessionMessage> future )
		{
			this.future = future;
		}
		
		@Override
		public void run( SessionContext ctx ) throws FaultException, ExitingException
		{
			System.out.println( "Try get message..." );
			try {
				SessionMessage m = future.get();
				if ( m != null ) {
					ctx.executeNext( receiveMessage( m, ctx ) );
				} else if ( !future.isDone() && !future.isCancelled() ) {
					ctx.executeNext( this );
					ctx.pauseExecution();
				}
				// If it is null, we got killed by a fault
			} catch( Exception e ) {
				ctx.interpreter().logSevere( e );
			}
		}

		@Override
		public Process clone( TransformationReason reason )
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		@Override
		public boolean isKillable()
		{
			throw new UnsupportedOperationException( "Not supported yet." );
		}
		
	}
	
	private final RequestResponseOperation operation;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Process process;
	private boolean isSessionStarter = false;
	
	public RequestResponseProcess(
			RequestResponseOperation operation,
			VariablePath inputVarPath,
			Expression outputExpression,
			Process process )
	{
		this.operation = operation;
		this.inputVarPath = inputVarPath;
		this.process = process;
		this.outputExpression = outputExpression;
	}
	
	public void setSessionStarter( boolean isSessionStarter )
	{
		this.isSessionStarter = isSessionStarter;
	}

	public InputOperation inputOperation()
	{
		return operation;
	}

	private void log( String log, CommMessage message )
	{
		final Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new MessageTraceAction(
			MessageTraceAction.Type.REQUEST_RESPONSE,
			operation.id(),
			log,
			message
		) );
	}
	
	public boolean isKillable()
	{
		return true;
	}

	public Process clone( TransformationReason reason )
	{
		return new RequestResponseProcess(
					operation,
					( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
					( outputExpression == null ) ? null : (VariablePath)outputExpression.cloneExpression( reason ),
					process.clone( reason )
				);
	}
	
	@Override
	public Process receiveMessage( final SessionMessage sessionMessage, SessionContext ctx )
	{
		if ( ctx.interpreter().isMonitoring() && !isSessionStarter ) {
			ctx.interpreter().fireMonitorEvent( new OperationStartedEvent( operation.id(), ctx.getSessionId(), Long.toString(sessionMessage.message().id()), sessionMessage.message().value() ) );
		}

		log( "RECEIVED", sessionMessage.message() );
		if ( inputVarPath != null ) {
			inputVarPath.getValue( ctx.state().root() ).refCopy( sessionMessage.message().value() );
		}

		return new Process() {
			
			@Override
			public void run( SessionContext ctx )
				throws FaultException, ExitingException
			{
				runBehaviour( ctx, sessionMessage.channel(), sessionMessage.message() );
			}

			@Override
			public Process clone( TransformationReason reason )
			{
				return this;
			}

			@Override
			public boolean isKillable()
			{
				return false;
			}
		};
	}

	@Override
	public void run(SessionContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		Future< SessionMessage > f = ctx.requestMessage( operation, ctx );
		ctx.executeNext( new RequestResponseListenProcess( f ) );
	}
	
	public VariablePath inputVarPath()
	{
		return inputVarPath;
	}

	private CommMessage createFaultMessage( CommMessage request, FaultException f )
		throws TypeCheckingException
	{
		if ( operation.typeDescription().faults().containsKey( f.faultName() ) ) {
			Type faultType = operation.typeDescription().faults().get( f.faultName() );
			if ( faultType != null ) {
				faultType.check( f.value() );
			}
		} else {			
			Interpreter.getInstance().logSevere(
				"Request-Response process for " + operation.id() +
				" threw an undeclared fault for that operation (" + f.faultName() + "), throwing TypeMismatch" );
			f = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error" );
		}
		return CommMessage.createFaultResponse( request, f );
	}
	
	private void runBehaviour( SessionContext ctx, CommChannel channel, CommMessage message )
		throws FaultException
	{
		// Variables for monitor
		int responseStatus;
		String details;

		FaultException typeMismatch = null;
		FaultException fault = null;
		CommMessage response;
		try {
			try {
				process.run( ctx );
			} catch( ExitingException e ) {}
			if ( ctx.isKilled() ) {
				try {
					response = createFaultMessage( message, ctx.killerFault() );
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
		} catch( FaultException f ) {
			try {
				response = createFaultMessage( message, f );
				responseStatus = OperationEndedEvent.FAULT;
				details = f.faultName();
			} catch( TypeCheckingException e ) {
				typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + f.faultName() + " (operation " + operation.id() + "): " + e.getMessage() );				
				response = CommMessage.createFaultResponse( message, typeMismatch );
				responseStatus = OperationEndedEvent.ERROR;
				details = typeMismatch.faultName();
			}
			fault = f;
		}

		try {
			channel.send( response, ctx );
			Value monitorValue;
			if ( response.isFault() ) {
				log( "SENT FAULT", response );					
				monitorValue = response.fault().value();
			} else {
				log( "SENT", response );
				monitorValue = response.value();
			}
			if ( Interpreter.getInstance().isMonitoring() ) {
				Interpreter.getInstance().fireMonitorEvent( new OperationEndedEvent( operation.id(), ExecutionThread.currentThread().getSessionId(), Long.toString( response.id() ), responseStatus, details, monitorValue ));
			}
		} catch( IOException e ) {
			//Interpreter.getInstance().logSevere( e );
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} finally {
			try {
				channel.release(); // TODO: what if the channel is in disposeForInput?
			} catch( IOException e ) {
				Interpreter.getInstance().logSevere( e );
			}
		}

		if ( fault != null ) {
			if ( typeMismatch != null ) {
				Interpreter.getInstance().logWarning( typeMismatch.value().strValue() );
			}
			throw fault;
		} else if ( typeMismatch != null ) {
			throw typeMismatch;
		}
	}
}
