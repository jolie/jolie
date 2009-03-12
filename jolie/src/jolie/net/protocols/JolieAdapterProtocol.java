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

package jolie.net.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URI;
import jolie.Interpreter;
import jolie.SessionListener;
import jolie.SessionThread;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.process.TransformationReason;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.util.Pair;

public class JolieAdapterProtocol extends ConcurrentCommProtocol
{
	/*final public boolean isThreadSafe()
	{
		return adaptor.executionMode() == Constants.ExecutionMode.CONCURRENT;
	}*/

	static private class InputProtocolConfigurationPathLazyHolder {
		final private static Pair< Expression, Expression >[] inputProtocolConfigurationPath;
		static {
			inputProtocolConfigurationPath = new Pair[] {
				new Pair< Expression, Expression >( Value.create( Constants.INPUT_PORTS_NODE_NAME ), Value.create( 0 ) ),
				new Pair< Expression, Expression >( Value.create( "Input" ), Value.create( 0 ) ),
				new Pair< Expression, Expression >( Value.create( Constants.PROTOCOL_NODE_NAME ), Value.create( 0 ) )
			};
		}
	}

	final private CommChannel adaptorChannel;
	final private Interpreter adaptor;
	final private URI location;

	private class SendListener implements SessionListener
	{
		private boolean terminated = false;
		private FaultException fault = null;
		public void sessionExecuted( SessionThread session )
		{
			synchronized( this ) {
				terminated = true;
				this.notify();
			}
		}

		public void sessionError( SessionThread session, FaultException fault )
		{
			synchronized( this ) {
				terminated = true;
				this.fault = fault;
				this.notify();
			}
		}
	}

	public String name()
	{
		return name;
	}

	final private String name;

	public JolieAdapterProtocol(
		VariablePath configurationPath,
		URI location,
		CommChannel adaptorChannel,
		Interpreter adaptor
	) {
		super( configurationPath );
		this.adaptorChannel = adaptorChannel;
		this.adaptor = adaptor;
		this.location = location;
		this.name = configurationPath.evaluate().strValue();
	}

	private CommMessage send_getAdaptedMessage( CommMessage message )
		throws IOException
	{
		Value requestValue = Value.create();
		requestValue.getFirstChild( "operationName" ).setValue( message.operationName() );
		requestValue.getFirstChild( "resourcePath" ).setValue( message.resourcePath() );
		requestValue.getFirstChild( "value" ).refCopy( message.value() );
		if ( message.isFault() ) {
			Value fault = requestValue.getFirstChild( "fault" );
			fault.getFirstChild( "value" ).refCopy( message.fault().value() );
			fault.setValue( message.fault().faultName() );
		}
		requestValue.getFirstChild( "protocol" ).deepCopy( configurationPath().getValue() );
		requestValue.getFirstChild( "protocol" ).setValue( (Object)null );

		CommMessage request = CommMessage.createRequest( "send", "/", requestValue );
		adaptorChannel.send( request );
		return adaptorChannel.recvResponseFor( request );
	}

	public void send( final OutputStream ostream, CommMessage message, final InputStream istream )
		throws IOException
	{
		CommMessage adaptedMessage = send_getAdaptedMessage( message );
		FaultException fault = null;
		if ( adaptedMessage.value().hasChildren( "fault" ) ) {
			Value adaptedFaultValue = adaptedMessage.value().getFirstChild( "fault" );
			fault = new FaultException(
						adaptedFaultValue.strValue(),
						adaptedFaultValue.getFirstChild( "value" )
					);
		}

		final CommMessage messageToSend = new CommMessage(
			message.id(),
			adaptedMessage.value().getFirstChild( "operationName" ).strValue(),
			adaptedMessage.value().getFirstChild( "resourcePath" ).strValue(),
			adaptedMessage.value().getFirstChild( "value" ),
			fault
		);

		final Value protocolValue = adaptedMessage.value().getFirstChild( "protocol" );

		jolie.process.Process sendingProcess = new jolie.process.Process() {
			public boolean isKillable()
			{
				return false;
			}

			public jolie.process.Process clone( TransformationReason reason )
			{
				return this;
			}

			public void run()
				throws FaultException
			{
				try {
					CommProtocol protocol = adaptor.commCore().createCommProtocol(
						protocolValue.strValue(),
						new ClosedVariablePath( VariablePath.EmptyPathLazyHolder.emptyPath, protocolValue ),
						location
					);
					protocol.setChannel( channel() );
					protocol.send( ostream, messageToSend, istream );
				} catch( IOException e ) {
					throw new FaultException( e );
				}
			}
		};

		SessionThread sendingThread = new SessionThread( sendingProcess, adaptor.mainThread() );
		SendListener listener = new SendListener();
		sendingThread.addSessionListener( listener );
		sendingThread.start();
		synchronized( listener ) {
			if ( listener.terminated == false ) {
				try {
					listener.wait();
				} catch( InterruptedException e ) {
					Interpreter.getInstance().logSevere( e );
				}
			}
		}
		if ( listener.fault != null ) {
			throw new IOException( listener.fault );
		}
	}

	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		RecvProcess recvProcess = new RecvProcess( istream, ostream );
		new SessionThread( recvProcess, adaptor.mainThread() ).start();
		synchronized( recvProcess ) {
			if ( recvProcess.terminated == false ) {
				try {
					recvProcess.wait();
				} catch( InterruptedException e ) {
					Interpreter.getInstance().logSevere( e );
				}
			}
		}
		if ( recvProcess.message == null ) {
			throw recvProcess.exception;
		}
		
		CommMessage adaptedMessage = recv_getAdaptedMessage( recvProcess.message );
		FaultException fault = null;
		if ( adaptedMessage.value().hasChildren( "fault" ) ) {
			Value adaptedFaultValue = adaptedMessage.value().getFirstChild( "fault" );
			fault = new FaultException(
				adaptedFaultValue.strValue(),
				adaptedFaultValue.getFirstChild( "value" )
			);
		}

		return new CommMessage(
			recvProcess.message.id(),
			adaptedMessage.value().getFirstChild( "operationName" ).strValue(),
			adaptedMessage.value().getFirstChild( "resourcePath" ).strValue(),
			adaptedMessage.value().getFirstChild( "value" ),
			fault
		);
	}

	private CommMessage recv_getAdaptedMessage( CommMessage message )
		throws IOException
	{
		Value requestValue = Value.create();
		requestValue.getFirstChild( "operationName" ).setValue( message.operationName() );
		requestValue.getFirstChild( "resourcePath" ).setValue( message.resourcePath() );
		requestValue.getFirstChild( "value" ).refCopy( message.value() );
		if ( message.isFault() ) {
			Value fault = requestValue.getFirstChild( "fault" );
			fault.getFirstChild( "value" ).refCopy( message.fault().value() );
			fault.setValue( message.fault().faultName() );
		}
		CommMessage request = CommMessage.createRequest( "recv", "/", requestValue );
		adaptorChannel.send( request );
		return adaptorChannel.recvResponseFor( request );
	}

	private class RecvProcess implements jolie.process.Process
	{
		final private InputStream istream;
		final private OutputStream ostream;
		private CommMessage message = null;
		private IOException exception = null;
		private boolean terminated = false;

		public RecvProcess( InputStream istream, OutputStream ostream )
		{
			this.istream = istream;
			this.ostream = ostream;
		}

		public boolean isKillable()
		{
			return false;
		}

		public jolie.process.Process clone( TransformationReason reason )
		{
			return this;
		}

		public void run()
			throws FaultException
		{
			try {
				VariablePath protocolConfigurationPath = new ClosedVariablePath(
					InputProtocolConfigurationPathLazyHolder.inputProtocolConfigurationPath,
					adaptor.globalValue()
				);
				CommProtocol protocol = adaptor.commCore().createCommProtocol(
					protocolConfigurationPath.evaluate().strValue(),
					protocolConfigurationPath,
					location
				);
				protocol.setChannel( channel() );
				synchronized( this ) {
					message = protocol.recv( istream, ostream );
					terminated = true;
					this.notify();
				}
			} catch( IOException e ) {
				synchronized( this ) {
					exception = e;
					terminated = true;
					this.notify();
				}
			}
		}
	}
}