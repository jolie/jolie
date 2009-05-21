/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package joliex.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.net.CommMessage;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class StringUtils extends JavaService
{
	public CommMessage length( CommMessage request )
	{
		return CommMessage.createResponse( request, Value.create( request.value().strValue().length() ) );
	}

	public CommMessage replaceAll( CommMessage message )
	{
		String regex = message.value().getChildren( "regex" ).first().strValue();
		String replacement = message.value().getChildren( "replacement" ).first().strValue();
		return CommMessage.createResponse(
			message,
			Value.create( message.value().strValue().replaceAll( regex, replacement ) )
		);
	}

	public CommMessage join( CommMessage request )
	{
		ValueVector vec = request.value().getChildren( "piece" );
		int size = vec.size() - 1;
		StringBuilder builder = new StringBuilder();
		if ( size >= 0 ) {
			String delimiter = request.value().getFirstChild( "delimiter" ).strValue();
			int i;
			for( i = 0; i < size; i++ ) {
				builder.append( vec.get( i ).strValue() ).append( delimiter );
			}
			builder.append( vec.get( i ).strValue() );
		}
		return CommMessage.createResponse( request, Value.create( builder.toString() ) );
	}
	
	public CommMessage trim( CommMessage message )
	{
		return CommMessage.createResponse( message, Value.create( message.value().strValue().trim() ) );
	}
	
	public CommMessage split( CommMessage message )
	{
		String str = message.value().strValue();
		int limit = 0;
		Value lValue = message.value().getFirstChild( "limit" );
		if ( lValue.isDefined() ) {
			limit = lValue.intValue();
		}
		String[] ss = str.split(
				message.value().getFirstChild( "regex" ).strValue(),
				limit
			);
		Value value = Value.create();
		for( int i = 0; i < ss.length; i++ ) {
			value.getNewChild( "result" ).add( Value.create( ss[ i ] ) );
		}

		return CommMessage.createResponse( message, value );
	}

	public CommMessage splitByLength( CommMessage request )
	{
		String str = request.value().strValue();
		int length = request.value().getFirstChild( "length" ).intValue();
		Value responseValue = Value.create();
		ValueVector result = responseValue.getChildren( "result" );
		int stringLength = str.length();
		boolean keepRun = true;
		int offset = 0;
		while( keepRun ) {
			if ( offset + length >= stringLength ) {
				keepRun = false;
				length = stringLength - offset;
			}
			result.add( Value.create( str.substring( offset, offset += length ) ) );
		}

		return CommMessage.createResponse( request, responseValue );
	}

	public CommMessage match( CommMessage message )
	{
		Pattern p = Pattern.compile( message.value().getFirstChild( "regex" ).strValue() );
		Matcher m = p.matcher( message.value().strValue() );
		Value response = Value.create();
		if ( m.matches() ) {
			response.setValue( 1 );
			ValueVector groups = response.getChildren( "group" );
			groups.add( Value.create( m.group( 0 ) ) );
			for( int i = 0; i < m.groupCount(); i++ ) {
				groups.add( Value.create( m.group( i+1 ) ) );
			}
		} else {
			response.setValue( 0 );
		}
		return CommMessage.createResponse( message, response );
	}

	public CommMessage leftPad( CommMessage request )
	{
		String orig = request.value().strValue();
		int length = request.value().getFirstChild( "length" ).intValue();
		if ( orig.length() >= length ) {
			return CommMessage.createResponse( request, Value.create( orig ) );
		}

		char padChar = request.value().getFirstChild( "char" ).strValue().charAt( 0 );

		StringBuilder builder = new StringBuilder();
		int padLength = orig.length() - length;
		for( int i = 0; i < padLength; i++ ) {
			builder.append( padChar );
		}
		builder.append( orig );
		return CommMessage.createResponse( request, Value.create( builder.toString() ) );
	}
}
