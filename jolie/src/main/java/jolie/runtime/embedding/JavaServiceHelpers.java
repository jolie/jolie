/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie.runtime.embedding;

import jolie.runtime.ByteArray;
import jolie.runtime.Value;

/**
 *
 * @author Fabrizio Montesi
 */
public class JavaServiceHelpers
{
	public static Value createValue( Value value )
	{
		return value;
	}
	
	public static Long valueToLong( Value value )
	{
		return value.longValue();
	}
	
	public static Boolean valueToBoolean( Value value )
	{
		return value.boolValue();
	}

	public static Integer valueToInteger( Value value )
	{
		return value.intValue();
	}

	public static String valueToString( Value value )
	{
		return value.strValue();
	}

	public static Double valueToDouble( Value value )
	{
		return value.doubleValue();
	}

	public static ByteArray valueToByteArray( Value value )
	{
		return value.byteArrayValue();
	}
}
