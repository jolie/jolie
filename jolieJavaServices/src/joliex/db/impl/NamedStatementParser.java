/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.db.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import jolie.runtime.Value;

/**
 * Commodity class for converting a parametrized
 * query in a valid JDBC {@link PreparedStatement}.
 *
 * @author Fabrizio Montesi
 * @see PreparedStatement
 */
public class NamedStatementParser
{
	private final Map< String, List< Integer > > parameterPositions = new HashMap< String, List< Integer > >();
	private final PreparedStatement statement;

	public NamedStatementParser( Connection connection, String sql, Value parameters )
		throws SQLException
	{
		String jdbcSql = parse( sql );
		statement = connection.prepareStatement( jdbcSql );
		Value v;
		for( Entry< String, List< Integer > > entry : parameterPositions.entrySet() ) {
			v = parameters.getFirstChild( entry.getKey() );
			if ( v.isInt() ) {
				for( Integer index : entry.getValue() ) {
					statement.setInt( index, v.intValue() );
				}
			} else if ( v.isDouble() ) {
				for( Integer index : entry.getValue() ) {
					statement.setDouble( index, v.doubleValue() );
				}
			} else if ( v.isByteArray() ) {
				for( Integer index : entry.getValue() ) {
					statement.setBytes( index, v.byteArrayValue().getBytes() );
				}
			} else {
				for( Integer index : entry.getValue() ) {
					statement.setString( index, v.strValue() );
				}
			}
		}
	}

	private String parse( String sql )
	{
		int length = sql.length();
		int index = 1;
		boolean inDoubleQuote = false;
		boolean inSingleQuote = false;
		char c;
		StringBuilder builder = new StringBuilder( length );
		int j;
		String name;
		List< Integer > positions;

		for( int i = 0; i < length; i++ ) {
			c = sql.charAt( i );
			if ( inSingleQuote ) {
				if ( c == '\'' ) {
					inSingleQuote = false;
				}
			} else if ( inDoubleQuote ) {
				if ( c == '"' ) {
					inDoubleQuote = false;
				}
			} else {
				if ( c == '\'' ) {
					inSingleQuote = true;
				} else if ( c == '"' ) {
					inDoubleQuote = true;
				} else if (
					c == ':' && i+1 < length && Character.isJavaIdentifierPart( sql.charAt( i+1 ) )
					&& ( i == 0 || sql.charAt( i - 1 ) != ':' )
				) {
					j = i + 2;
					while( j < length && Character.isJavaIdentifierPart( sql.charAt( j ) ) ) {
						j++;
					}
					name = sql.substring( i + 1, j );
					c = '?';
					i += name.length();

					positions = getParameterPositions( name );
					positions.add( index++ );
				}
			}

			builder.append( c );
		}

		return builder.toString();
	}

	private List< Integer > getParameterPositions( String parameterName )
	{
		List< Integer > ret = parameterPositions.get( parameterName );
		if ( ret == null ) {
			ret = new ArrayList< Integer >();
			parameterPositions.put( parameterName, ret );
		}
		return ret;
	}

	public PreparedStatement getPreparedStatement()
	{
		return statement;
	}
}
