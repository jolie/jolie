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
import java.nio.channels.SelectionKey;
import jolie.Interpreter;
import jolie.net.protocols.CommProtocol;

abstract public class SelectableStreamingCommChannel extends StreamingCommChannel
{
	public SelectableStreamingCommChannel( URI location, CommProtocol protocol )
	{
		super( location, protocol );
	}

	abstract public InputStream inputStream();
	abstract public SelectableChannel selectableChannel();

	private SelectionKey selectionKey = null;

	public SelectionKey selectionKey()
	{
		return selectionKey;
	}

	public void setSelectionKey( SelectionKey selectionKey )
	{
		this.selectionKey = selectionKey;
	}

	@Override
	public void send( CommMessage message )
		throws IOException
	{
		synchronized( channelMutex ) {
			if ( selectionKey != null ) {
				final CommCore commCore = Interpreter.getInstance().commCore();
				commCore.unregisterForSelection( this );
				sendImpl( message );
				commCore.registerForSelection( this );
			} else {
				sendImpl( message );
			}
		}
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException
	{
		synchronized( channelMutex ) { // We do not want sendings or receivings during this.
			if ( selectionKey == null ) {
				Interpreter.getInstance().commCore().registerForSelection( this );
			}
		}
	}
}
