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
import jolie.runtime.expression.Expression;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.tracer.MessageTraceAction;
import jolie.tracer.Tracer;

public class RequestResponseBehaviour implements InputOperationBehaviour
{
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
	public Behaviour receiveMessage( final SessionMessage sessionMessage, StatefulContext ctx )
	{
		if ( ctx.interpreter().isMonitoring() && !isSessionStarter ) {
			ctx.interpreter().fireMonitorEvent( new OperationStartedEvent( operation.id(), ctx.getSessionId(), Long.toString(sessionMessage.message().id()), sessionMessage.message().value() ) );
		}

		log( ctx.interpreter(), "RECEIVED", sessionMessage.message() );
		if ( inputVarPath != null ) {
			inputVarPath.getValue( ctx.state().root() ).refCopy( sessionMessage.message().value() );
		}

		return new SequentialBehaviour(new Behaviour[] {
			process,
			new SimpleBehaviour() {
				
				@Override
				public void run( StatefulContext ctx )
					throws FaultException, ExitingException
				{
					runBehaviour( ctx, sessionMessage.channel(), sessionMessage.message() );
				}
				
			}
		});
			
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
	
	private void runBehaviour( StatefulContext ctx, CommChannel channel, CommMessage message )
		throws FaultException
	{
		// Variables for monitor
		int responseStatus;
		String details;

		FaultException typeMismatch = null;
		FaultException fault = null;
		CommMessage response;
//		try {
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
//		} catch( FaultException f ) {
//			try {
//				response = createFaultMessage( message, f );
//				responseStatus = OperationEndedEvent.FAULT;
//				details = f.faultName();
//			} catch( TypeCheckingException e ) {
//				typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + f.faultName() + " (operation " + operation.id() + "): " + e.getMessage() );				
//				response = CommMessage.createFaultResponse( message, typeMismatch );
//				responseStatus = OperationEndedEvent.ERROR;
//				details = typeMismatch.faultName();
//			}
//			fault = f;
//		}

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
						new OperationEndedEvent(operation.id(), ctx.getSessionId(), Long.toString( msg.id() ), rStatus, fDetails, monitorValue ));
				}
				System.out.println( "ReuestResponseBehaviour - SEND" );
				return null;
			});

		} catch( IOException e ) {
			//Interpreter.getInstance().logSevere( e );
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} 

//		if ( fault != null ) {
//			if ( typeMismatch != null ) {
//				ctx.interpreter().logWarning( typeMismatch.value().strValue() );
//			}
//			throw fault;
//		} else 
		if ( typeMismatch != null ) {
			throw typeMismatch;
		}
	}
}
