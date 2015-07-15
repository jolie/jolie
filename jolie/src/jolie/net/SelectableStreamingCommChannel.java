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

package jolie.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.SelectableChannel;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;

/**
 * This class implements the support for a selectable channel.
 * A channel implementation based on this class must provide
 * methods for accessing its receiving <code>InputStream</code> and
 * <code>SelectableChannel</code>.
 * @author Fabrizio Montesi
 */
public abstract class SelectableStreamingCommChannel extends StreamingCommChannel
{
	private static final long LIFETIME = 5000; // 5 secs
	
	private final long creationTime = System.currentTimeMillis();
	
	private int selectorIndex;
	
	public int selectorIndex()
	{
		return selectorIndex;
	}
	
	public void setSelectorIndex( int selectorIndex )
	{
		this.selectorIndex = selectorIndex;
	}
	
	public SelectableStreamingCommChannel( URI location, CommProtocol protocol )
	{
		super( location, protocol );
	}

	/**
	 * Returns the receiving <code>InputStream</code> of this channel.
	 * @return the receiving <code>InputStream</code> of this channel
	 */
	abstract public InputStream inputStream();

	/**
	 * Returns the receiving <code>SelectableChannel</code> of this channel.
	 * @return the receiving <code>SelectableChannel</code> of this channel
	 */
	abstract public SelectableChannel selectableChannel();

	@Override
	public final void send( CommMessage message )
		throws IOException
	{
		if ( lock.isHeldByCurrentThread() ) {
			_send( message );
		} else {
			lock.lock();
			try {
				_send( message );
			} finally {
				lock.unlock();
			}
		}
	}

	private void _send( CommMessage message )
		throws IOException
	{
		final CommCore commCore = Interpreter.getInstance().commCore();
		if ( commCore.isSelecting( this ) ) {
			commCore.unregisterForSelection( this );
			sendImpl( message );
			commCore.registerForSelection( this );
		} else {
			sendImpl( message );
		}
	}

	private void _disposeForInputImpl()
		throws IOException
	{
		Interpreter.getInstance().commCore().registerForSelection( this );
	}


	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		if ( lock.isHeldByCurrentThread() ) {
			_disposeForInputImpl();
		} else {
			lock.lock();
			try {
				_disposeForInputImpl();
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	protected void releaseImpl()
		throws IOException
	{
		if ( lock.isHeldByCurrentThread() ) {
			final CommCore commCore = Interpreter.getInstance().commCore();
			if ( commCore.isSelecting( this ) == false ) {
				super.releaseImpl();
			} else if ( System.currentTimeMillis() - creationTime > LIFETIME ) {
				commCore.unregisterForSelection( this );
				setToBeClosed( true );
				close();
			}
		} else {
			lock.lock();
			try {
				final CommCore commCore = Interpreter.getInstance().commCore();
				if ( commCore.isSelecting( this ) == false ) {
					super.releaseImpl();
				} else if ( System.currentTimeMillis() - creationTime > LIFETIME ) {
					commCore.unregisterForSelection( this );
					setToBeClosed( true );
					close();
				}
			} finally {
				lock.unlock();
			}
		}
	}
}
