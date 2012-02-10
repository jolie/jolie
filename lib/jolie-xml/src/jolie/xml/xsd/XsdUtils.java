/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2010 by Mirco Gamberini                                 *
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


package jolie.xml.xsd;

import java.util.HashMap;
import java.util.Map;
import jolie.lang.NativeType;

/**
 *
 * @author Fabrizio Montesi
 */
public class XsdUtils
{
	private final static Map< String, NativeType > xsdToNativeTypeMap;

	static {
		xsdToNativeTypeMap = new HashMap< String, NativeType >();
		xsdToNativeTypeMap.put( "string", NativeType.STRING );
		xsdToNativeTypeMap.put( "date", NativeType.STRING );
		xsdToNativeTypeMap.put( "time", NativeType.STRING );
		xsdToNativeTypeMap.put( "boolean", NativeType.BOOL );
		xsdToNativeTypeMap.put( "int", NativeType.INT );
		xsdToNativeTypeMap.put( "integer", NativeType.INT );
		xsdToNativeTypeMap.put( "decimal", NativeType.DOUBLE );
		xsdToNativeTypeMap.put( "anyType", NativeType.ANY );
		xsdToNativeTypeMap.put( "dateTime", NativeType.STRING );
	}

	public static NativeType xsdToNativeType( String xsdTypeName )
	{
		return xsdToNativeTypeMap.get( xsdTypeName );
	}
}
