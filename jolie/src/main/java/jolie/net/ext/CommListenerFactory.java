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

package jolie.net.ext;

import java.io.IOException;

import jolie.Interpreter;
import jolie.net.CommCore;
import jolie.net.CommListener;
import jolie.net.ports.InputPort;

/**
 * A factory for delegating the creation of communication listeners to extensions.
 *
 * @author Fabrizio Montesi
 */
public abstract class CommListenerFactory {
	private final CommCore commCore;

	protected CommListenerFactory( CommCore commCore ) {
		this.commCore = commCore;
	}

	protected final CommCore commCore() {
		return commCore;
	}

	/**
	 * Creates and returns a valid communication listener.
	 *
	 * @param interpreter the interpreter to refer to
	 * @param protocolFactory the protocol factory the listener has to use
	 * @param inputPort the input port for this listener
	 * @return a valid communication listener
	 * @throws java.io.IOException if the listener could not be created
	 */
	abstract public CommListener createListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort )
		throws IOException;
}
