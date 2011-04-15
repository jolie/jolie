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

package jolie.net.http;

import java.util.Map.Entry;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author Fabrizio Montesi
 */
public class JsonTransformer
{
	private final static String ROOT_SIGN = "$";

	public static void valueToJsonString( Value value, StringBuilder builder )
	{
		builder.append( '{' );
		if ( value.isDefined() ) {
			appendKeyColon( builder, ROOT_SIGN );
			builder.append( nativeValueToJsonString( value ) );
		}
		int size = value.children().size();
		int i = 0;
		for( Entry< String, ValueVector > child : value.children().entrySet() ) {
			if ( child.getValue().isEmpty() == false ) {
				appendKeyColon( builder, child.getKey() );
				valueVectorToJsonString( child.getValue(), builder );
			}
			if ( i++ < size - 1 ) {
				builder.append( ',' );
			}
		}
		builder.append( '}' );
	}

	private static void valueVectorToJsonString( ValueVector vector, StringBuilder builder )
	{
		if ( vector.size() > 1 ) {
			builder.append( '[' );
			for( int i = 0; i < vector.size(); i++ ) {
				valueToJsonString( vector.get( i ), builder );
				if ( i < vector.size() - 1 ) {
					builder.append( ',' );
				}
			}
			builder.append( ']' );
		} else {
			valueToJsonString( vector.first(), builder );
		}
	}

	private static void appendKeyColon( StringBuilder builder, String key )
	{
		builder.append( '"' )
			.append( key )
			.append( "\":" );
	}

	private static String nativeValueToJsonString( Value value )
	{
		if ( value.isInt() || value.isDouble() ) {
			return value.strValue();
		} else {
			return '"' + value.strValue() + '"';
		}
	}
}
