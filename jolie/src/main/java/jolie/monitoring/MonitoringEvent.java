/***************************************************************************
 *   Copyright (C) 2012 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.monitoring;

import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;

/**
 * A monitoring event, supporting the {@link ValueConverter} interface for automatic transformation
 * between Jolie messages and Java objects.
 *
 * @author Fabrizio Montesi
 */
public class MonitoringEvent implements ValueConverter {
	private final String type;
	private final long timestamp;
	private final long memory;
	private final Value data;

	public MonitoringEvent( String type, Value data ) {
		this(
			type,
			System.currentTimeMillis(),
			Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
			data );
	}

	private MonitoringEvent( String type, long timestamp, long memory, Value data ) {
		this.type = type;
		this.timestamp = timestamp;
		this.memory = memory;
		this.data = data;
	}

	public String type() {
		return type;
	}

	public long timestamp() {
		return timestamp;
	}

	public long memory() {
		return memory;
	}

	public Value data() {
		return data;
	}

	public static MonitoringEvent fromValue( Value value ) {
		return new MonitoringEvent(
			value.getFirstChild( "type" ).strValue(),
			value.getFirstChild( "timestamp" ).longValue(),
			value.getFirstChild( "memory" ).longValue(),
			value.getFirstChild( "data" ) );
	}

	public static Value toValue( MonitoringEvent e ) {
		Value ret = Value.create();
		ret.getFirstChild( "type" ).setValue( e.type );
		ret.getFirstChild( "timestamp" ).setValue( e.timestamp );
		ret.getFirstChild( "memory" ).setValue( e.memory );
		ret.getChildren( "data" ).add( e.data );
		return ret;
	}
}
