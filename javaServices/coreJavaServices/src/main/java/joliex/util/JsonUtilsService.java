/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2013 by Claudio Guidi                                   *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

import java.io.IOException;
import java.io.StringReader;
import jolie.js.JsUtils;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;

/**
 *
 * @author claudio guidi
 */
@AndJarDeps( { "jolie-js.jar", "json_simple.jar" } )
public class JsonUtilsService extends JavaService
{

	public Value getJsonString( Value request ) throws FaultException
	{
		Value ret = Value.create();

		StringBuilder stringBuilder = new StringBuilder();
		try {
			JsUtils.valueToJsonString( request, true, Type.UNDEFINED, stringBuilder );
			ret.setValue( stringBuilder.toString() );
		} catch( IOException e ) {
			throw new FaultException( "JSON string generation from Jolie value failed" );
		}

		return ret;
	}

	public Value getJsonValue( Value request ) throws FaultException
	{
		Value ret = Value.create();

		String charset = null;
		if ( request.hasChildren( "charset" ) ) {
			charset = request.getFirstChild( "charset" ).strValue();
		}

		try {
			String str;
			if ( request.isByteArray() && charset != null ) {
				str = new String( request.byteArrayValue().getBytes(), charset );
			} else {
				str = request.strValue();
			}
			JsUtils.parseJsonIntoValue( new StringReader( str ), ret, request.getFirstChild( "strictEncoding" ).boolValue() );
		} catch( IOException e ) {
			throw new FaultException( "Jolie value generation from JSON string failed" );
		}

		return ret;
	}
}
