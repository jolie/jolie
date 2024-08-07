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

import java.util.Map;

import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

/**
 * Represents a (runtime) interface extender
 *
 * @author Fabrizio Montesi
 */
public class InterfaceExtender extends Interface {
	private final OneWayTypeDescription defaultOneWayTypeDescription;
	private final RequestResponseTypeDescription defaultRequestResponseTypeDescription;

	public InterfaceExtender(
		Map< String, OneWayTypeDescription > oneWayOperations,
		Map< String, RequestResponseTypeDescription > requestResponseOperations,
		OneWayTypeDescription defaultOneWayTypeDescription,
		RequestResponseTypeDescription defaultRequestResponseTypeDescription ) {
		super( oneWayOperations, requestResponseOperations );
		this.defaultOneWayTypeDescription = defaultOneWayTypeDescription;
		this.defaultRequestResponseTypeDescription = defaultRequestResponseTypeDescription;
	}

	public OneWayTypeDescription defaulOneWayTypeDescription() {
		return defaultOneWayTypeDescription;
	}

	public RequestResponseTypeDescription defaultRequestResponseTypeDescription() {
		return defaultRequestResponseTypeDescription;
	}

	public OneWayTypeDescription getOneWayTypeDescription( String operationName ) {
		OneWayTypeDescription ret = oneWayOperations().get( operationName );
		if( ret == null ) {
			ret = defaultOneWayTypeDescription;
		}
		return ret;
	}

	public RequestResponseTypeDescription getRequestResponseTypeDescription( String operationName ) {
		RequestResponseTypeDescription ret = requestResponseOperations().get( operationName );
		if( ret == null ) {
			ret = defaultRequestResponseTypeDescription;
		}
		return ret;
	}
}
