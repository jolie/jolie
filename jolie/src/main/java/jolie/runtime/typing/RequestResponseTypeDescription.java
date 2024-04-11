/***************************************************************************
 *   Copyright (C) 2009-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.runtime.typing;

import java.util.Map;

/**
 *
 * @author Fabrizio Montesi
 */
public class RequestResponseTypeDescription implements OperationTypeDescription {
	private final Type requestType;
	private final Type responseType;
	private final Map< String, Type > faultTypes;

	public RequestResponseTypeDescription( Type requestType, Type responseType, Map< String, Type > faultTypes ) {
		this.requestType = requestType;
		this.responseType = responseType;
		this.faultTypes = faultTypes;
	}

	public Type requestType() {
		return requestType;
	}

	public Type responseType() {
		return responseType;
	}

	public Type getFaultType( String faultName ) {
		return faultTypes.get( faultName );
	}

	public Map< String, Type > faults() {
		return faultTypes;
	}

	@Override
	public OneWayTypeDescription asOneWayTypeDescription() {
		return null;
	}

	@Override
	public RequestResponseTypeDescription asRequestResponseTypeDescription() {
		return this;
	}
}
