/*
 * Copyright (C) 2007-2024 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.net;

import java.io.IOException;
import jolie.Interpreter;
import jolie.net.ext.CommListenerFactory;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

public class LocalSocketListenerFactory extends CommListenerFactory {
	public LocalSocketListenerFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort )
		throws IOException {
		return new LocalSocketListener( interpreter, protocolFactory, inputPort );
	}
}
