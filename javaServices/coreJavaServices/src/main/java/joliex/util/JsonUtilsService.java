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
import java.util.Set;

import com.networknt.schema.*;
import jolie.js.JsUtils;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.typing.Type;

/**
 *
 * @author claudio guidi
 */
@AndJarDeps( { "jolie-js.jar", "json-simple.jar", "json-schema-validator.jar", "slf4j-api.jar",
	"jackson-databind.jar", "jackson-core.jar", "jackson-dataformat-yaml.jar", "jackson-annotation.jar" } )
public class JsonUtilsService extends JavaService {

	public Value getJsonString( Value request ) throws FaultException {
		Value ret = Value.create();

		StringBuilder stringBuilder = new StringBuilder();
		try {
			JsUtils.valueToJsonString( request, true, Type.UNDEFINED, stringBuilder );
			ret.setValue( stringBuilder.toString() );
		} catch( IOException e ) {
			throw new FaultException( "JSONCreationError" );
		}

		return ret;
	}

	public Value getJsonValue( Value request ) throws FaultException {
		Value ret = Value.create();

		String charset = null;
		if( request.hasChildren( "charset" ) ) {
			charset = request.getFirstChild( "charset" ).strValue();
		}

		try {
			String str;
			if( request.isByteArray() && charset != null ) {
				str = new String( request.byteArrayValue().getBytes(), charset );
			} else {
				str = request.strValue();
			}
			JsUtils.parseJsonIntoValue( new StringReader( str ), ret,
				request.getFirstChild( "strictEncoding" ).boolValue() );
		} catch( IOException e ) {
			throw new FaultException( "JSONCreationError" );
		}

		return ret;
	}

	@RequestResponse
	public Value validateJson( Value request ) {
		String schemaStr = request.getFirstChild( "schema" ).strValue();
		String json = request.getFirstChild( "json" ).strValue();
		Value response = Value.create();
		JsonSchema schema = JsonSchemaFactory.getInstance( SpecVersion.VersionFlag.V202012 ).getSchema( schemaStr,
			SchemaValidatorsConfig.builder().build() );
		Set< ValidationMessage > assertions = schema.validate( json, InputFormat.JSON, executionContext -> {
			executionContext.getExecutionConfig().setFormatAssertionsEnabled( true );
		} );

		for( ValidationMessage validationMessage : assertions.stream().toList() ) {
			Value message = Value.create();
			message.getFirstChild( "message" ).setValue( validationMessage.getMessage() );
			message.getFirstChild( "code" ).setValue( validationMessage.getCode() );
			message.getFirstChild( "error" ).setValue( validationMessage.getError() );
			message.getFirstChild( "type" ).setValue( validationMessage.getType() );
			response.getChildren( "validationMessage" ).add( message );
		}

		return response;
	}
}
