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

package joliex.monitoring;

import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;

/**
 * A monitoring event, supporting the {@link ValueConverter} interface for automatic
 * transformation between Jolie messages and Java objects.
 * @author Fabrizio Montesi
 */
public class MonitoringEvent implements ValueConverter
{
	private final String name;
	private final long timestamp;
	private final Value data;
	
	public MonitoringEvent( String name, long timestamp, Value data )
	{
		this.name = name;
		this.timestamp = timestamp;
		this.data = data;
	}
	
	public String name()
	{
		return name;
	}
	
	public long timestamp()
	{
		return timestamp;
	}
	
	public Value data()
	{
		return data;
	}
	
	public static MonitoringEvent fromValue( Value value )
	{
		return new MonitoringEvent(
			value.getFirstChild( "name" ).strValue(),
			value.getFirstChild( "timestamp" ).longValue(),
			value.getFirstChild( "data" )
		);
	}
	
	public static Value toValue( MonitoringEvent e )
	{
		Value ret = Value.create();
		ret.getFirstChild( "name" ).setValue( e.name );
		ret.getFirstChild( "timestamp" ).setValue( e.timestamp );
		ret.getChildren( "data" ).add( e.data );
		return ret;
	}
}
