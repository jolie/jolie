/***************************************************************************
 *   Copyright (C) 2019 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2019 by Eros Fabrici <eros.fabrici@gmail.com>           *
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

package jolie.net;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Eros Fabrici
 */
public class LSPMessage {

	private byte[] content = null;
	final private Map< String, String > propMap = new HashMap<>();

	public String getProperty( String name ) {
		String property = getPropMap().get( name.toLowerCase() );
		if( property != null )
			property = property.trim();
		return property;
	}

	protected Map< String, String > getPropMap() {
		return propMap;
	}

	public void setContent( byte[] content ) {
		this.content = content;
	}

	public void setProperty( String name, String value ) {
		propMap.put( name.toLowerCase(), value );
	}

	public Collection< Entry< String, String > > getProperties() {
		return propMap.entrySet();
	}

	public int size() {
		if( content == null )
			return 0;
		return content.length;
	}

	public byte[] content() {
		return content;
	}
}
