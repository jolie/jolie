/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2011 by Károly Szántó                                   *
 *   Copyright (C) 2011 by Giannakis Manthios                              *
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

package jolie.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import jolie.net.http.json.JsonUtils;
import jolie.net.protocols.ConcurrentCommProtocol;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 * 
 * @author Fabrizio Montesi
 * @author Károly Szántó
 * @author Giannakis Manthios
 */
public class JsonRpcProtocol extends ConcurrentCommProtocol
{
	private final boolean inInputPort;
	
	public String name()
	{
		return "jsonrpc";
	}

	public JsonRpcProtocol( VariablePath configurationPath, boolean inInputPort )
	{
		super( configurationPath );
		this.inInputPort = inInputPort;
	}

	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		if ( checkBooleanParameter( "keepAlive", true ) ) {
			channel().setToBeClosed( false );
		} else {
			channel().setToBeClosed( true );
		}
		
		Value value = Value.create();
		value.getFirstChild( "id" ).setValue( message.id() );
		value.getFirstChild( "jsonrpc" ).setValue( "2.0" );
		if ( message.isFault() ) {
			Value error = value.getFirstChild( "error" );
			error.getFirstChild( "code" ).setValue( -32000 );
			error.getFirstChild( "message" ).setValue( message.fault().faultName() );
			error.getChildren( "data" ).set( 0, message.fault().value() );
		} else {
			if ( inInputPort ) {
				if ( message.value().isDefined() ) {
					value.getChildren( "result" ).set( 0, message.value() );
				}
			} else {
				value.getFirstChild( "method" ).setValue( message.operationName() );
				if ( message.value().isDefined() ) {
					value.getChildren( "params" ).set( 0, message.value() );
				}
			}
		}
		StringBuilder builder = new StringBuilder();
		JsonUtils.valueToJsonString( value, builder );
		ostream.write( builder.toString().getBytes() );
	}

	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		if ( checkBooleanParameter( "keepAlive", true ) ) {
			channel().setToBeClosed( false );
		} else {
			channel().setToBeClosed( true );
		}
		
		Value value = Value.create();
		JsonUtils.parseJsonIntoValue( new InputStreamReader( istream ), value );

		long id = value.getFirstChild( "id" ).intValue();
		String operationName = value.getFirstChild( "method" ).strValue();
		if ( inInputPort ) {
			return new CommMessage( id, operationName, "/", value.getFirstChild( "params" ), null );
		} else if ( value.hasChildren( "result" ) ) {
			return new CommMessage( id, operationName, "/", value.getFirstChild( "result" ), null );
		} else if ( value.hasChildren( "error" ) ) {
			return new CommMessage( id, operationName, "/",
				value.getFirstChild( "result" ),
				new FaultException(
					value.getFirstChild( "error" ).getFirstChild( "message" ).strValue(),
					value.getFirstChild( "error" ).getFirstChild( "data" )
				)
			);
		}

		throw new IOException( "Received JSON-RPC message does not specify a result, a method call or an error." );
	}
}