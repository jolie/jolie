/**
 * *************************************************************************
 * Copyright (C) by Claudio Guidi * * This program is free software; you can
 * redistribute it and/or modify * it under the terms of the GNU Library General
 * Public License as * published by the Free Software Foundation; either version
 * 2 of the * License, or (at your option) any later version. * * This program
 * is distributed in the hope that it will be useful, * but WITHOUT ANY
 * WARRANTY; without even the implied warranty of * MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the * GNU General Public License for more
 * details. * * You should have received a copy of the GNU Library General
 * Public * License along with this program; if not, write to the * Free
 * Software Foundation, Inc., * 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA. * * For details about the authors of this software, see the
 * AUTHORS file. *
 * *************************************************************************
 */
package joliex.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Map.Entry;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 *
 * @author claudio guidi
 */
@AndJarDeps({"json_simple.jar"})
public class JsonUtilsService extends JavaService {
    
    public Value getJsonString( Value request ) throws FaultException {
        Value ret = Value.create();
        
        StringBuilder stringBuilder = new StringBuilder();
        try {
            valueToJsonString( request, stringBuilder);
            ret.setValue( stringBuilder.toString() );
        } catch( IOException e ) {
            throw new FaultException("JSONCreationError" );
        }
       
        return ret;
    }
    
    	private final static String ROOT_SIGN = "$";
        private final static String JSONARRAY_KEY = "_";
        private final static String JSONULL = "__NULL__";

	private static void valueToJsonString( Value value, StringBuilder builder )
		throws IOException
	{
		if ( value.children().isEmpty() ) {
			if ( value.isDefined() ) {
				builder.append( nativeValueToJsonString( value ) );
			} else {
                                builder.append("{}");
                        }
		} else {
                        if ( value.hasChildren( JSONARRAY_KEY ))  {
                            valueVectorToJsonString( value.children().get( JSONARRAY_KEY ), builder, true );
                        } else {
                                builder.append( '{' );
                                if ( value.isDefined() ) {
                                        appendKeyColon( builder, ROOT_SIGN );
                                        builder.append( nativeValueToJsonString( value ) );
                                }
                                int size = value.children().size();
                                int i = 0;
                                for( Entry< String, ValueVector > child : value.children().entrySet() ) {                                
                                        appendKeyColon( builder, child.getKey() );
                                        valueVectorToJsonString( child.getValue(), builder, false );                                
                                        if ( i++ < size - 1 ) {
                                                builder.append( ',' );
                                        }
                                }
                                builder.append( '}' );
                        }
		}	
		
	}
        
        private static void appendKeyColon( StringBuilder builder, String key )
	{
		builder.append( '"' )
			.append( key )
			.append( "\":" );
	}
        
        private static void valueVectorToJsonString( ValueVector vector, StringBuilder builder, boolean isArray )
		throws IOException
	{
                if ( isArray || ( !isArray && vector.size() > 1 )) {
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

	private static String nativeValueToJsonString( Value value )
		throws IOException
	{
		if ( value.isInt() || value.isDouble() ) {
			return value.strValue();
		} else {
                    if ( value.strValue().equals( JSONULL )) {
                        return "null";
                    } else {
			return '"' + JSONValue.escape( value.strValue() ) + '"';
                    }
		}
	}
}