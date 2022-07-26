/*
 * Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package jolie.uri;

import jolie.runtime.Value;

import java.net.MalformedURLException;
import java.util.Map;
import org.springframework.web.util.UriTemplate;

public class UriUtils {
	public static Value match( Value request ) {
		return match( request.getFirstChild( "template" ).strValue(), request.getFirstChild( "uri" ).strValue() );
	}

	public static Value match( String template, String uri ) {
		UriTemplate t = new UriTemplate( template );
		Value response = Value.create();
		boolean matches = t.matches( uri );
		response.setValue( matches );
		if( matches ) {
			Map< String, String > map = t.match( uri );
			for( Map.Entry< String, String > res : map.entrySet() ) {
				response.setFirstChild( res.getKey(), res.getValue() );
			}
		}
		return response;
	}

	public static String expand( String template, Map< String, Object > params ) throws MalformedURLException {
		try {
			UriTemplate t = new UriTemplate( template );
			return t.expand( params ).toString();
		} catch( IllegalArgumentException e ) {
			throw new MalformedURLException( e.getMessage() );
		}
	}



}
