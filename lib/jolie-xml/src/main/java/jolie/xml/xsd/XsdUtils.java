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
public class XsdUtils {
	private final static Map< String, NativeType > XSD_TO_NATIVE_TYPE_MAP;

	static {
		XSD_TO_NATIVE_TYPE_MAP = new HashMap<>();
		XSD_TO_NATIVE_TYPE_MAP.put( "string", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "date", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "time", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "boolean", NativeType.BOOL );
		XSD_TO_NATIVE_TYPE_MAP.put( "int", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "long", NativeType.LONG );
		XSD_TO_NATIVE_TYPE_MAP.put( "unsignedLong", NativeType.LONG );
		XSD_TO_NATIVE_TYPE_MAP.put( "integer", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "nonNegativeInteger", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "negativeInteger", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "nonPositiveInteger", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "positiveInteger", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "unsignedShort", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "unsignedInt", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "short", NativeType.INT );
		XSD_TO_NATIVE_TYPE_MAP.put( "decimal", NativeType.DOUBLE );
		XSD_TO_NATIVE_TYPE_MAP.put( "anyType", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "anySimpleType", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "dateTime", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "time", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "duration", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "date", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "dateTime", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "gMonthDay", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "gYearDay", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "gDay", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "gMonth", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "gYearMonth", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "gYear", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "anyURI", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "normalizedString", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "base64Binary", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "base64", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "byte", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "unsignedByte", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "hexBinary", NativeType.ANY );
		XSD_TO_NATIVE_TYPE_MAP.put( "float", NativeType.DOUBLE );
		XSD_TO_NATIVE_TYPE_MAP.put( "double", NativeType.DOUBLE );
		XSD_TO_NATIVE_TYPE_MAP.put( "decimal", NativeType.DOUBLE );
		XSD_TO_NATIVE_TYPE_MAP.put( "ENTITIES", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "ENTITY", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "ID", NativeType.STRING ); // A string that represents the ID attribute in XML (only
																// used with schema attributes)
		XSD_TO_NATIVE_TYPE_MAP.put( "IDREF", NativeType.STRING ); // A string that represents the IDREF attribute in XML
																	// (only used with schema attributes)
		XSD_TO_NATIVE_TYPE_MAP.put( "IDREFS", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "language", NativeType.STRING ); // A string that contains a valid language id
		XSD_TO_NATIVE_TYPE_MAP.put( "Name", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "NCName", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "NMTOKEN", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "NMTOKENS", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "normalizedString", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "QName", NativeType.STRING );
		XSD_TO_NATIVE_TYPE_MAP.put( "token", NativeType.STRING );
	}

	public static NativeType xsdToNativeType( String xsdTypeName ) {
		return XSD_TO_NATIVE_TYPE_MAP.get( xsdTypeName );
	}
}
