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

import jolie.net.CommCore;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.VariablePath;

/**
 * A factory for delegating the creation of protocol instances to extensions.
 *
 * @author Fabrizio Montesi
 */
abstract public class CommProtocolFactory {
	final private CommCore commCore;

	/**
	 * Constructor
	 *
	 * @param commCore the CommCore to refer to for creating CommProtocol instances.
	 */
	public CommProtocolFactory( CommCore commCore ) {
		this.commCore = commCore;
	}

	protected CommCore commCore() {
		return commCore;
	}

	/**
	 * Creates a CommProtocol instance meant for an input port.
	 *
	 * @param configurationPath the configuration VariablePath the returned CommProtocol must refer to
	 * @param location the location the returned CommProtocol must refer to
	 * @return a CommProtocol instance
	 * @throws java.io.IOException
	 */
	abstract public CommProtocol createInputProtocol( VariablePath configurationPath, URI location )
		throws IOException;

	/**
	 * Creates a CommProtocol instance meant for an output port.
	 *
	 * @param configurationPath the configuration VariablePath the returned CommProtocol must refer to
	 * @param location the location the returned CommProtocol must refer to
	 * @return a CommProtocol instance
	 * @throws java.io.IOException
	 */
	abstract public CommProtocol createOutputProtocol( VariablePath configurationPath, URI location )
		throws IOException;
}
