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
import java.net.URI;

import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.ports.OutputPort;

/**
 * A factory for delegating the creation of output communication channels to extensions.
 *
 * @author Fabrizio Montesi
 */
public abstract class CommChannelFactory {
	final private CommCore commCore;

	protected CommChannelFactory( CommCore commCore ) {
		this.commCore = commCore;
	}

	final protected CommCore commCore() {
		return commCore;
	}

	/**
	 * Creates and returns a communication channel.
	 *
	 * @param location the location URI to use
	 * @param port the output port to refer to in the creation of the channel
	 * @return a valid communication channel
	 * @throws java.io.IOException if the channel could not be created
	 */
	abstract public CommChannel createChannel( URI location, OutputPort port )
		throws IOException;
}
