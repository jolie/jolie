/**********************************************************************************
 *   Copyright (C) 2017-18 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017-18 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

package jolie.net;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import jolie.net.coap.communication.codec.CoapMessageDecoder;
import jolie.net.coap.communication.codec.CoapMessageEncoder;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;

/**
Implementations of {@link AsyncCommProtocol} CoAP for Jolie.
-------------------------------------------------------------------------------------
1. COAP MESSAGE INBOUND					CoapDecoder
2. COAP MESSAGE OUTBOUND					CoapEncoder
3. COAP MESSAGE INBOUND/OUTBOUND			CoapToCommMessageCodec
4. COMM MESSAGE INBOUND					StreamingCommChannelHandler
5. TIMEOUT INBOUND/OUTBOUND				ReadTimeoutHandler
6. ERROR INBOUND							CoapToCommMessageCodec
-------------------------------------------------------------------------------------
INBOUND read( 1 -> 3 -> 4 -> 5 -> 6 )
	ByteBuf	->		CoapMessage	->		CommMessage	-> CommMessage
-------------------------------------------------------------------------------------
OUTBOUND write( 3 -> 2 -> 5 )
	CommMessage	->		CoapMessage	->		ByteBuf
-------------------------------------------------------------------------------------
@author stefanopiozingaro
 */
public class CoapProtocol extends AsyncCommProtocol
{

	public boolean isInput;

	/**
	 *
	 * @param configurationPath
	 * @param isInput
	 */
	public CoapProtocol( VariablePath configurationPath, boolean isInput )
	{
		super( configurationPath );
		this.isInput = isInput;
	}

	@Override
	public void setupPipeline( ChannelPipeline pipeline )
	{
		pipeline.addLast( "LOGGER", new LoggingHandler( LogLevel.INFO ) );
		pipeline.addLast( "COAP MESSAGE INBOUND", new CoapMessageDecoder() );
		pipeline.addLast( "COAP MESSAGE OUTBOUND", new CoapMessageEncoder() );
		pipeline.addLast( "COAP MESSAGE INBOUND/OUTBOUND", new CoapToCommMessageCodec( this ) );
	}

	@Override
	public String name()
	{
		return "coap";
	}

	@Override
	public boolean isThreadSafe()
	{
		return true;
	}

	@Override
	protected boolean checkBooleanParameter( String param )
	{
		return super.checkBooleanParameter( param );
	}

	@Override
	protected boolean hasOperationSpecificParameter( String on, String p )
	{
		return super.hasOperationSpecificParameter( on, p );
	}

	@Override
	protected ValueVector getOperationSpecificParameterVector( String operationName,
		String parameterName )
	{
		return super.getOperationSpecificParameterVector( operationName,
			parameterName );
	}

	@Override
	protected String getOperationSpecificStringParameter( String on, String p )
	{
		return super.getOperationSpecificStringParameter( on, p );
	}

	@Override
	protected Value getOperationSpecificParameterFirstValue( String on,
		String p )
	{
		return super.getOperationSpecificParameterFirstValue( on, p );
	}

	@Override
	protected boolean checkStringParameter( String id, String value )
	{
		return super.checkStringParameter( id, value );
	}

	@Override
	protected boolean hasParameter( String id )
	{
		return super.hasParameter( id );
	}

	@Override
	protected int getIntParameter( String id )
	{
		return super.getIntParameter( id );
	}

	@Override
	protected CommChannel channel()
	{
		return super.channel();
	}

	@Override
	protected Type getSendType( String message ) throws IOException
	{
		return super.getSendType( message );
	}

	@Override
	protected void setSendExecutionThread( Long k )
	{
		super.setSendExecutionThread( k ); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	protected <K> void setReceiveExecutionThread( K k )
	{
		super.setReceiveExecutionThread( k ); //To change body of generated methods, choose Tools | Templates.
	}

	/**
	 * Given the <code>alias</code> for an operation, it searches iteratively in
	 * the <code>configurationPath</code> of the {@link AsyncCommProtocol} to
	 * find the corresponsding <code>operationName</code>.
	 *
	 * @param parameter
	 * @param parameterStringValue
	 * @return The operation name String
	 */
	public String getOperationFromOperationSpecificStringParameter( String parameter,
		String parameterStringValue )
	{

		for( Map.Entry<String, ValueVector> first : configurationPath().getValue().children().entrySet() ) {
			String first_level_key = first.getKey();
			ValueVector first_level_valueVector = first.getValue();
			if ( first_level_key.equals( "osc" ) ) {
				for( Iterator<Value> first_iterator = first_level_valueVector.iterator(); first_iterator.hasNext(); ) {
					Value fisrt_value = first_iterator.next();
					for( Map.Entry<String, ValueVector> second : fisrt_value.children().entrySet() ) {
						String second_level_key = second.getKey();
						ValueVector second_level_valueVector = second.getValue();
						for( Iterator<Value> second_iterator = second_level_valueVector.iterator(); second_iterator.hasNext(); ) {
							Value second_value = second_iterator.next();
							for( Map.Entry<String, ValueVector> third : second_value.children().entrySet() ) {
								String third_level_key = third.getKey();
								ValueVector third_level_valueVector = third.getValue();
								if ( third_level_key.equals( parameter ) ) {
									StringBuilder sb = new StringBuilder( "" );
									for( Iterator<Value> third_iterator = third_level_valueVector.iterator(); third_iterator.hasNext(); ) {
										Value third_value = third_iterator.next();
										sb.append( third_value.strValue() );
									}
									if ( sb.toString().equals( parameterStringValue ) ) {
										return second_level_key;
									}
								}
							}
						}
					}
				}
			}
		}
		return parameterStringValue;
	}
}
