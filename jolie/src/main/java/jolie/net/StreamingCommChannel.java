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
import java.net.URI;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.CommProtocol;

/**
 * This abstract class implements a communication channel based on a <code>CommProtocol</code>.
 * 
 * @author Fabrizio Montesi
 * @see SelectableStreamingCommChannel
 */
public abstract class StreamingCommChannel extends AbstractCommChannel {
	private final URI location;
	private final CommProtocol protocol;

	public StreamingCommChannel( URI location, CommProtocol protocol ) {
		this.location = location;
		this.protocol = protocol;
		protocol.setChannel( this );
	}

	protected CommProtocol protocol() {
		return protocol;
	}

	@Override
	protected boolean isThreadSafe() {
		return protocol.isThreadSafe();
	}

	@Override
	protected void releaseImpl()
		throws IOException {
		// Helpers.lockAndThen( lock,
		// () -> {
		if( parentPort() instanceof OutputPort ) {
			((OutputPort) parentPort()).putPersistentChannel( location, protocol.name(), this );
		}
		// } );
	}
}
