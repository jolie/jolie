/***************************************************************************
 *   Copyright (C) 2010-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.storage.types;

import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;

/**
 *
 * @author Fabrizio Montesi
 */
public class SaveRequest implements ValueConverter
{
	private final StoragePath storagePath;
	private final Value value;

	private SaveRequest( StoragePath storagePath, Value value )
	{
		this.storagePath = storagePath;
		this.value = value;
	}

	public StoragePath storagePath()
	{
		return storagePath;
	}

	public Value value()
	{
		return value;
	}

	public static SaveRequest fromValue( Value value )
	{
		return new SaveRequest(
			StoragePath.fromValue( value.getFirstChild( "path" ) ),
			value
		);
	}

	public static Value toValue( SaveRequest request )
	{
		Value ret = Value.create();
		ret.getChildren( "path" ).add( StoragePath.toValue( request.storagePath ) );
		ret.getChildren( "value" ).add( request.value );
		return ret;
	}
}
