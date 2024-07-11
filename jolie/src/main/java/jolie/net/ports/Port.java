/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.net.ports;

import jolie.runtime.VariablePath;
import jolie.runtime.typing.OperationTypeDescription;

/**
 *
 * @author Fabrizio Montesi
 */
public interface Port {
	/**
	 * Returns the {@link VariablePath} pointing to the protocol configuration of this port.
	 *
	 * @return the {@link VariablePath} pointing to the protocol configuration of this port.
	 */
	VariablePath protocolConfigurationPath();

	/**
	 * Returns the {@link Interface} exposed directly by this port.
	 *
	 * @return the {@link Interface} exposed directly by this port.
	 */
	Interface getInterface();

	/**
	 * Returns the {@link OperationTypeDescription} of the named operation, also considering the type
	 * modifications given by eventual aggregations.
	 *
	 * @param operationName the name of the operation.
	 * @param resourcePath the resource path of the operation (in case of redirection).
	 * @return the {@link OperationTypeDescription} of the named operation.
	 */
	OperationTypeDescription getOperationTypeDescription( String operationName, String resourcePath );
}
