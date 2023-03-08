/***************************************************************************
 *   Copyright (C) 2006-2008 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import jolie.Interpreter;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.ports.Port;
import jolie.util.Helpers;

/**
 * <code>CommChannel</code> allows for the sending and receiving of <code>CommMessage</code>
 * instances. This class is thread-safe.
 *
 * This abstract class is meant to be extended by classes implementing the communication logic for
 * sending and receiving messages.
 *
 * Either the {@link #disposeForInput() disposeForInput} or the {@link #release() release} method
 * must be called after using a channel. Their behaviour can be influenced by indicating if a
 * channel is to be closed or not through the {@link #setToBeClosed(boolean) setToBeClosed} method,
 * but this does not give any right to make assumptions on said behaviour: implementing classes have
 * complete freedom on that.
 * 
 * @author Fabrizio Montesi
 * @see CommMessage
 */
public abstract class CommChannel {
	protected final ReentrantLock rwLock = new ReentrantLock( false );

	private volatile boolean toBeClosed = true;
	private InputPort inputPort = null;
	private OutputPort outputPort = null;
	private boolean isOpen = true;

	private long redirectionMessageId = 0L;

	private final Object timeoutHandlerMutex = new Object();
	private Future< ? > timeoutHandler = null;

	protected boolean cancelTimeoutHandler() {
		boolean result;
		synchronized( timeoutHandlerMutex ) {
			// true if there is no handler or if we can cancel it
			result = timeoutHandler == null || timeoutHandler.cancel( false );
			timeoutHandler = null;
		}
		return result;
	}

	protected void setTimeoutHandler( Runnable newTimeoutHandler, Interpreter interpreter, long delay ) {
		synchronized( timeoutHandlerMutex ) {
			// If there is no current handler or if we are still in time to cancel the current handler
			if( timeoutHandler == null || timeoutHandler.cancel( false ) ) {
				timeoutHandler = interpreter.schedule( () -> {
					synchronized( timeoutHandlerMutex ) {
						newTimeoutHandler.run();
						this.timeoutHandler = null;
					}
				}, delay );
			}
		}
	}

	protected long redirectionMessageId() {
		return redirectionMessageId;
	}

	protected void setRedirectionMessageId( long id ) {
		redirectionMessageId = id;
	}

	private CommChannel redirectionChannel = null;

	/**
	 * Returns <code>true</code> if this channel is to be closed when not used, <code>false</code>
	 * otherwise.
	 * 
	 * @return <code>true</code> if this channel is to be closed when not used, <code>false</code>
	 *         otherwise.
	 */
	public final boolean toBeClosed() {
		return toBeClosed;
	}

	public CommChannel createDuplicate() {
		throw new IllegalAccessError( "Only local channels can be duplicated" );
	}

	/**
	 * Sets a redirection channel for this channel. When a <code>CommChannel</code> has a redirection
	 * channel set, the next input message received by the <code>CommCore</code> on that channel will be
	 * redirected automatically to the redirection channel that has been set.
	 * 
	 * @param redirectionChannel the redirection channel to set
	 */
	public void setRedirectionChannel( CommChannel redirectionChannel ) {
		this.redirectionChannel = redirectionChannel;
	}

	/**
	 * Returns the redirection channel of this channel.
	 * 
	 * @return the redirection channel of this channel
	 */
	public CommChannel redirectionChannel() {
		return redirectionChannel;
	}

	/**
	 * Sets the parent {@link InputPort} of this channel.
	 * 
	 * @param inputPort the parent {@link InputPort} of this channel.
	 */
	public void setParentInputPort( InputPort inputPort ) {
		this.inputPort = inputPort;
	}

	/**
	 * Returns the parent {@link InputPort} of this channel.
	 * 
	 * @return the parent {@link InputPort} of this channel.
	 */
	public InputPort parentInputPort() {
		return inputPort;
	}

	/**
	 * Sets the parent {@link OutputPort} of this channel.
	 * 
	 * @param outputPort the parent {@link OutputPort} of this channel.
	 */
	public void setParentOutputPort( OutputPort outputPort ) {
		this.outputPort = outputPort;
	}

	/**
	 * Returns the parent {@link Port} of this channel.
	 * 
	 * @return the parent {@link Port} of this channel.
	 */
	public Port parentPort() {
		return (inputPort == null) ? outputPort : inputPort;
	}

	/**
	 * Returns <code>true</code> if this channel is open, <code>false</code> otherwise.
	 * 
	 * @return <code>true</code> if this channel is open, <code>false</code> otherwise
	 */
	public final boolean isOpen() {
		boolean result;
		if( rwLock.tryLock() ) {
			result = isOpen && isOpenImpl();
			rwLock.unlock();
		} else {
			result = true;
		}
		return result;
	}

	protected boolean isOpenImpl() {
		return true;
	}

	protected boolean isThreadSafe() {
		return false;
	}

	/**
	 * Receives a message from the channel. This is a blocking operation.
	 * 
	 * @return the received message
	 * @throws IOException in case of some communication error
	 */
	public CommMessage recv()
		throws IOException {
		return Helpers.lockAndThen( rwLock, this::recvImpl );
	}

	/**
	 * Receives a response for the specified request.
	 * 
	 * @param request the request message for which we want to receive a response
	 * @return the response for the specified request message
	 * @throws java.io.IOException in case of some communication error
	 */
	public abstract Future< CommMessage > recvResponseFor( CommMessage request )
		throws IOException;

	/**
	 * Sends a message through this channel.
	 * 
	 * @param message the message to send
	 * @throws java.io.IOException in case of some communication error
	 */
	public void send( final CommMessage message )
		throws IOException {
		try {
			Helpers.lockAndThen( rwLock, () -> sendImpl( message ) );
		} catch( IOException e ) {
			setToBeClosed( true );
			throw e;
		}
	}

	protected abstract CommMessage recvImpl()
		throws IOException;

	protected abstract void sendImpl( CommMessage message )
		throws IOException;

	/**
	 * Releases this CommChannel, making it available to other processes for sending data.
	 * 
	 * @throws IOException in case of an internal error
	 *
	 */
	public final void release()
		throws IOException {
		if( toBeClosed() ) {
			close();
		} else {
			releaseImpl();
		}
	}

	protected void releaseImpl()
		throws IOException {
		close();
	}

	/**
	 * Disposes this channel for input. This method can behave in two ways, depending on the state of
	 * the channel and its underlying implementation:
	 * <ul>
	 * <li>the channel is closed and its resources are released;</li>
	 * <li>the channel is kept open and its control is given to its generating <code>CommCore</code>
	 * instance, which will listen for input messages on this channel.</li>
	 * </ul>
	 * 
	 * @throws java.io.IOException in case of some error generated by this channel implementation
	 */
	public final void disposeForInput()
		throws IOException {
		// TODO: might be useless locking
		Helpers.lockAndThen( rwLock, () -> {
			if( toBeClosed() == false ) {
				disposeForInputImpl();
			} /*
				 * else { close(); }
				 */
		} );
	}

	protected void disposeForInputImpl()
		throws IOException {}

	/**
	 * Sets if this channel is to be closed after releasing or not.
	 * 
	 * @param toBeClosed <code>true</code> if this channel is to be closed after releasing,
	 *        <code>false</code> otherwise
	 */
	public void setToBeClosed( boolean toBeClosed ) {
		this.toBeClosed = toBeClosed;
	}

	protected final void close()
		throws IOException {
		isOpen = false;
		closeImpl();
	}

	/**
	 * Implements the communication channel closing operation.
	 * 
	 * @throws java.io.IOException
	 */
	protected abstract void closeImpl()
		throws IOException;
}
