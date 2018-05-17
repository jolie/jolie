/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net;

import io.netty.channel.ChannelPipeline;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

import jolie.net.coap.communication.codec.CoapMessageDecoder;
import jolie.net.coap.communication.codec.CoapMessageEncoder;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;

/**
 * Implementations of {@link AsyncCommProtocol} CoAP for Jolie.
 *
 * @author stefanopiozingaro
 */
public class CoapProtocol extends AsyncCommProtocol {

	public boolean isInput;

	/**
	 *
	 * @param configurationPath
	 * @param isInput
	 */
	public CoapProtocol( VariablePath configurationPath, boolean isInput ) {
		super( configurationPath );
		this.isInput = isInput;
	}

	@Override
	public void setupPipeline( ChannelPipeline p ) {
//    p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
		p.addLast( "DECODER", new CoapMessageDecoder() );
		p.addLast( "ENCODER", new CoapMessageEncoder() );
		p.addLast( "CODEC", new CoapToCommMessageCodec( this ) );
	}

	@Override
	public String name() {
		return "coap";
	}

	@Override
	public boolean isThreadSafe() {
		return true;
	}

	@Override
	protected boolean checkBooleanParameter( String param ) {
		return super.checkBooleanParameter( param );
	}

	@Override
	protected boolean hasOperationSpecificParameter( String on, String p ) {
		return super.hasOperationSpecificParameter( on, p );
	}

	@Override
	protected ValueVector getOperationSpecificParameterVector( String operationName,
		String parameterName ) {
		return super.getOperationSpecificParameterVector( operationName,
			parameterName );
	}

	@Override
	protected String getOperationSpecificStringParameter( String on, String p ) {
		return super.getOperationSpecificStringParameter( on, p );
	}

	@Override
	protected Value getOperationSpecificParameterFirstValue( String on,
		String p ) {
		return super.getOperationSpecificParameterFirstValue( on, p );
	}

	@Override
	protected boolean checkStringParameter( String id, String value ) {
		return super.checkStringParameter( id, value );
	}

	@Override
	protected CommChannel channel() {
		return super.channel();
	}

	@Override
	protected Type getSendType( String message ) throws IOException {
		return super.getSendType( message );
	}

	/**
	 * Given the <code>alias</code> for an operation, it searches iteratively in
	 * the <code>configurationPath</code> of the {@link AsyncCommProtocol} to find
	 * the corresponsding <code>operationName</code>.
	 *
	 * @param alias the alias for the wanted operation
	 * @return The operation name String
	 */
	public String getOperationFromOSC( String parameter, String parameterStringValue ) {

		for ( Map.Entry<String, ValueVector> first : configurationPath().getValue().children().entrySet() ) {
			String first_level_key = first.getKey();
			ValueVector first_level_valueVector = first.getValue();
			if ( first_level_key.equals( "osc" ) ) {
				for ( Iterator<Value> first_iterator = first_level_valueVector.iterator(); first_iterator.hasNext(); ) {
					Value fisrt_value = first_iterator.next();
					for ( Map.Entry<String, ValueVector> second : fisrt_value.children().entrySet() ) {
						String second_level_key = second.getKey();
						ValueVector second_level_valueVector = second.getValue();
						for ( Iterator<Value> second_iterator = second_level_valueVector.iterator(); second_iterator.hasNext(); ) {
							Value second_value = second_iterator.next();
							for ( Map.Entry<String, ValueVector> third : second_value.children().entrySet() ) {
								String third_level_key = third.getKey();
								ValueVector third_level_valueVector = third.getValue();
								if ( third_level_key.equals( parameter ) ) {
									StringBuilder sb = new StringBuilder( "" );
									for ( Iterator<Value> third_iterator = third_level_valueVector.iterator(); third_iterator.hasNext(); ) {
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

	private String valueToPrettyString( Value request ) {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( request, writer, "" );
		try {
			printer.run();
		} catch ( IOException e ) {
		} // Should never happen
		return writer.toString();

	}
}
