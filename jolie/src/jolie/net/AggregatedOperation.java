/***************************************************************************
 *   Copyright (C) 2009-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.net;

import java.io.IOException;
import java.net.URISyntaxException;
import jolie.Interpreter;
import jolie.SessionListener;
import jolie.State;
import jolie.StatefulContext;
import jolie.behaviours.Behaviour;
import jolie.behaviours.OneWayBehaviour;
import jolie.behaviours.RequestResponseBehaviour;
import jolie.behaviours.SequentialBehaviour;
import jolie.lang.Constants;
import jolie.lang.Constants.OperationType;
import jolie.net.ports.OutputPort;
import jolie.runtime.FaultException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.TypeCheckingException;

/**
 * An AggregatedOperation instance contains information about an operation that is aggregated by an input port.
 * @author Fabrizio Montesi
 */
public abstract class AggregatedOperation
{
	private static class CourierOneWayAggregatedOperation extends AggregatedOperation {
		private final OneWayOperation operation;
		private final Behaviour courierProcess;
		private final VariablePath inputVariablePath;
		
		public CourierOneWayAggregatedOperation(
			OneWayOperation operation,
			VariablePath inputVariablePath,
			Behaviour courierProcess
		) {
			super( operation.id() );
			this.operation = operation;
			this.inputVariablePath = inputVariablePath;
			this.courierProcess = courierProcess;
		}
		
		@Override
		public OperationType type()
		{
			return OperationType.ONE_WAY;
		}
		
		@Override
		public void runAggregationBehaviour( final StatefulContext ctx, final CommMessage requestMessage, final CommChannel channel )
			throws IOException, URISyntaxException
		{
			final Interpreter interpreter = ctx.interpreter();
			try {
				operation.requestType().check( requestMessage.value() );

				StatefulContext initContext = interpreter.initContext();
				try {
					initContext.join();
				} catch( InterruptedException e ) {
					throw new IOException( e );
				}

				State state = initContext.state().clone();
				Behaviour p = new SequentialBehaviour( new Behaviour[] {
					new OneWayBehaviour( operation, inputVariablePath ).receiveMessage( new SessionMessage( requestMessage, channel ), ctx ),
					courierProcess
				});
				StatefulContext c = new StatefulContext( p, state, initContext );
				
				final FaultException[] f = new FaultException[1];
				f[0] = null;
				c.addSessionListener(new SessionListener() {
					@Override
					public void onSessionExecuted( StatefulContext session )
					{}

					@Override
					public void onSessionError( StatefulContext session, FaultException fault )
					{
						// We need to send the acknowledgement
						if ( fault.faultName().equals( "CorrelationError" )
							|| fault.faultName().equals( "IOException" )
							|| fault.faultName().equals( "TypeMismatch" )
							) {
							synchronized( f ) {
								f[0] = fault;
							}
						} else {
							interpreter.logSevere( "Courier session for operation " + operation.id() + " has thrown fault " + fault.faultName() + ", which cannot be forwarded to the caller. Forwarding IOException." );
							synchronized( f ) {
								f[0] = new FaultException( jolie.lang.Constants.IO_EXCEPTION_FAULT_NAME, fault.faultName() );
							}
						}
					}
				} );
				c.start();
				try {
					c.join();
				} catch( InterruptedException e ) {}
				
				synchronized( f ) {
					if ( f[0] == null ) {
						// We need to send the acknowledgement
						channel.send( CommMessage.createEmptyResponse( requestMessage ), c );
					} else {
						channel.send( CommMessage.createFaultResponse( requestMessage, f[0] ), c );
					}
				}
			} catch( TypeCheckingException e ) {
				interpreter.logWarning( "TypeMismatch for received message (input operation " + operation.id() + "): " + e.getMessage() );
				try {
					channel.send( CommMessage.createFaultResponse( requestMessage, new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ), ctx );
				} catch( IOException ioe ) {
					interpreter.logSevere( ioe );
				}
			} finally {
				channel.disposeForInput();
			}
		}
		
		@Override
		public OperationTypeDescription getOperationTypeDescription()
		{
			return operation.getOneWayTypeDescription();
		}
	}
	
	private static class CourierRequestResponseAggregatedOperation extends AggregatedOperation {
		private final RequestResponseOperation operation;
		private final Behaviour courierProcess;
		private final VariablePath inputVariablePath;
		private final VariablePath outputVariablePath;
		
		public CourierRequestResponseAggregatedOperation(
			RequestResponseOperation operation,
			VariablePath inputVariablePath,
			VariablePath outputVariablePath,
			Behaviour courierProcess
		) {
			super( operation.id() );
			this.operation = operation;
			this.inputVariablePath = inputVariablePath;
			this.outputVariablePath = outputVariablePath;
			this.courierProcess = courierProcess;
		}
		
		@Override
		public OperationType type()
		{
			return OperationType.REQUEST_RESPONSE;
		}
		
		@Override
		public void runAggregationBehaviour( StatefulContext ctx, final CommMessage requestMessage, final CommChannel channel )
			throws IOException, URISyntaxException
		{
			final Interpreter interpreter = ctx.interpreter();
			try {
				operation.requestType().check( requestMessage.value() );

				StatefulContext initContext = interpreter.initContext();
				try {
					initContext.join();
				} catch( InterruptedException e ) {
					throw new IOException( e );
				}

				State state = initContext.state().clone();
				Behaviour p = new RequestResponseBehaviour( operation, inputVariablePath, outputVariablePath, courierProcess )
								.receiveMessage( new SessionMessage( requestMessage, channel ), ctx );
				new StatefulContext( p, state, initContext ).start();
			} catch( TypeCheckingException e ) {
				interpreter.logWarning( "Received message TypeMismatch (input operation " + operation.id() + "): " + e.getMessage() );
				try {
					channel.send( CommMessage.createFaultResponse( requestMessage, new FaultException( jolie.lang.Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ), ctx );
				} catch( IOException ioe ) {
					interpreter.logSevere( ioe );
				}
			} finally {
				channel.disposeForInput();
			}
		}

		@Override
		public OperationTypeDescription getOperationTypeDescription()
		{
			return operation.typeDescription();
		}
	}
	
	private static class DirectAggregatedOperation extends AggregatedOperation {
		private final OutputPort outputPort;
		private final Constants.OperationType type;
		private final String name;
		
		public DirectAggregatedOperation( String name, Constants.OperationType type, OutputPort outputPort )
		{
			super( name );
			this.type = type;
			this.outputPort = outputPort;
			this.name = name;
		}
		
		@Override
		public OperationType type()
		{
			return type;
		}
		
		@Override
		public void runAggregationBehaviour( StatefulContext ctx, CommMessage requestMessage, CommChannel channel )
			throws IOException, URISyntaxException
		{
			CommChannel oChannel = null;
			try {
				oChannel = outputPort.getNewCommChannel( ctx );
				final CommMessage requestToAggregated = outputPort.createAggregatedRequest( requestMessage );
				oChannel.send( requestToAggregated, ctx );
				final CommMessage response = oChannel.recvResponseFor( ctx, requestToAggregated );
				channel.send( new CommMessage( requestMessage.id(), response.operationName(), response.resourcePath(), response.value(), response.fault() ), ctx );
			} catch( IOException e ) {
				channel.send( CommMessage.createFaultResponse( requestMessage, new FaultException( e ) ), ctx );
			} finally {
				if ( oChannel != null ) {
					oChannel.close();
				}
				channel.disposeForInput();
			}
		}

		@Override
		public OperationTypeDescription getOperationTypeDescription()
		{
			if ( type == OperationType.ONE_WAY ) {
				return outputPort.getInterface().oneWayOperations().get( name );
			} else {
				return outputPort.getInterface().requestResponseOperations().get( name );
			}
		}
	}

	private final String name;
	
	private AggregatedOperation( String name )
	{
		this.name = name;
	}
	
	public static AggregatedOperation createDirect( String name, Constants.OperationType type, OutputPort outputPort )
	{
		return new DirectAggregatedOperation( name, type, outputPort );
	}
	
	public static AggregatedOperation createWithCourier(
		OneWayOperation operation,
		VariablePath inputVariablePath,
		Behaviour courierProcess
	) {
		return new CourierOneWayAggregatedOperation( operation, inputVariablePath, courierProcess );
	}
	
	public static AggregatedOperation createWithCourier(
		RequestResponseOperation operation,
		VariablePath inputVariablePath,
		VariablePath outputVariablePath,
		Behaviour courierProcess
	) {
		return new CourierRequestResponseAggregatedOperation( operation, inputVariablePath, outputVariablePath, courierProcess );
	}
	
	/**
	 * Returns the operation type of this operation
	 * @return the operation type of this operation
	 * @see OperationType
	 */
	public abstract OperationType type();

	public abstract OperationTypeDescription getOperationTypeDescription();

	/**
	 * Returns the name of this operation.
	 * @return the name of this operation.
	 */
	public String name()
	{
		return name;
	}
        
	public abstract void runAggregationBehaviour( StatefulContext ctx, CommMessage requestMessage, CommChannel channel )
		throws IOException, URISyntaxException;
}
