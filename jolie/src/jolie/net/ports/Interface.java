/***************************************************************************
 *   Copyright 2011-2015 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;

/**
 * Represents the (runtime) interface of a port, holding data about
 * the types of the operations defined within.
 * @author Fabrizio Montesi
 */
public class Interface
{
	private static final class UndefinedOneWayOperationsMap
		implements Map< String, OneWayTypeDescription >
	{
		private static final Set< Entry< String, OneWayTypeDescription > > entrySet =
			Collections.EMPTY_SET;
		private static final Collection< OneWayTypeDescription > values = Collections.EMPTY_LIST;
		private static final Set< String > keySet = Collections.EMPTY_SET;
		private static final OneWayTypeDescription value = new OneWayTypeDescription( Type.UNDEFINED );

		public Set< Entry< String, OneWayTypeDescription > > entrySet()
		{
			return entrySet;
		}

		public Collection< OneWayTypeDescription > values()
		{
			return values;
		}

		public Set< String > keySet()
		{
			return keySet;
		}

		public void clear()
		{}

		public void putAll( Map< ? extends String, ? extends OneWayTypeDescription > map )
		{
			throw new UnsupportedOperationException();
		}

		public OneWayTypeDescription remove( Object key )
		{
			throw new UnsupportedOperationException();
		}

		public OneWayTypeDescription put( String key, OneWayTypeDescription value )
		{
			throw new UnsupportedOperationException();
		}

		public OneWayTypeDescription get( Object key )
		{
			return value;
		}

		public boolean containsValue( Object value )
		{
			throw new UnsupportedOperationException();
		}

		public boolean containsKey( Object value )
		{
			return true;
		}

		public boolean isEmpty()
		{
			return false;
		}

		public int size()
		{
			throw new UnsupportedOperationException();
		}
	}

	private static final class UndefinedRequestResponseOperationsMap
		implements Map< String, RequestResponseTypeDescription >
	{
		private static final Set< Entry< String, RequestResponseTypeDescription > > entrySet = Collections.EMPTY_SET;
		private static final Collection< RequestResponseTypeDescription > values = Collections.EMPTY_LIST;
		private static final Set< String > keySet = Collections.EMPTY_SET;
		private static final RequestResponseTypeDescription value =
			new RequestResponseTypeDescription( Type.UNDEFINED, Type.UNDEFINED, Collections.EMPTY_MAP );

		public Set< Entry< String, RequestResponseTypeDescription > > entrySet()
		{
			return entrySet;
		}

		public Collection< RequestResponseTypeDescription > values()
		{
			return values;
		}

		public Set< String > keySet()
		{
			return keySet;
		}

		public void clear()
		{}

		public void putAll( Map< ? extends String, ? extends RequestResponseTypeDescription > map )
		{
			throw new UnsupportedOperationException();
		}

		public RequestResponseTypeDescription remove( Object key )
		{
			throw new UnsupportedOperationException();
		}

		public RequestResponseTypeDescription put( String key, RequestResponseTypeDescription value )
		{
			throw new UnsupportedOperationException();
		}

		public RequestResponseTypeDescription get( Object key )
		{
			return value;
		}

		public boolean containsValue( Object value )
		{
			throw new UnsupportedOperationException();
		}

		public boolean containsKey( Object value )
		{
			return true;
		}

		public boolean isEmpty()
		{
			return false;
		}

		public int size()
		{
			throw new UnsupportedOperationException();
		}
	}

	public static final Interface UNDEFINED;
	private static final Map< String, OneWayTypeDescription > UNDEFINED_ONE_WAY_MAP;
	private static final Map< String, RequestResponseTypeDescription > UNDEFINED_REQUEST_RESPONSE_MAP;

	static {
		UNDEFINED_ONE_WAY_MAP = new UndefinedOneWayOperationsMap();
		UNDEFINED_REQUEST_RESPONSE_MAP = new UndefinedRequestResponseOperationsMap();
		UNDEFINED = new Interface( UNDEFINED_ONE_WAY_MAP, UNDEFINED_REQUEST_RESPONSE_MAP );
	}

	private final Map< String, RequestResponseTypeDescription > requestResponseOperations;
	private final Map< String, OneWayTypeDescription > oneWayOperations;

	public Interface(
		Map< String, OneWayTypeDescription > oneWayOperations,
		Map< String, RequestResponseTypeDescription > requestResponseOperations
	) {
		this.oneWayOperations = oneWayOperations;
		this.requestResponseOperations = requestResponseOperations;
	}

	public Map< String, OneWayTypeDescription > oneWayOperations()
	{
		return oneWayOperations;
	}

	public Map< String, RequestResponseTypeDescription > requestResponseOperations()
	{
		return requestResponseOperations;
	}

	public void merge( Interface other )
	{
		oneWayOperations.putAll( other.oneWayOperations );
		requestResponseOperations.putAll( other.requestResponseOperations );
	}

	public boolean containsOperation( String operationName )
	{
		return oneWayOperations.containsKey( operationName ) ||
			requestResponseOperations.containsKey( operationName );
	}
}
