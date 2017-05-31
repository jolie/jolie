/********************************************************************************
 *   Copyright (C) 2011-2017 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2011 by Károly Szántó                                        *
 *   Copyright (C) 2011 by Giannakis Manthios                                   *
 *   Copyright (C) 2014 by Matthias Dieter Wallnöfer                            *
 *   Copyright (C) 2017 by Martin Møller Andersen <maan511@student.sdu.dk>      *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>  *
 *                                                                              *
 *   This program is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Library General Public License as            *
 *   published by the Free Software Foundation; either version 2 of the         *
 *   License, or (at your option) any later version.                            *
 *                                                                              *
 *   This program is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *   GNU General Public License for more details.                               *
 *                                                                              *
 *   You should have received a copy of the GNU Library General Public          *
 *   License along with this program; if not, write to the                      *
 *   Free Software Foundation, Inc.,                                            *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                  *
 *                                                                              *
 *   For details about the authors of this software, see the AUTHORS file.      *
 ********************************************************************************/

package jolie.net;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jolie.Interpreter;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.UnsupportedMethodException;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.js.JsUtils;
import jolie.runtime.typing.Type;

public class JsonRpcProtocol extends AsyncCommProtocol
{
    private final static EncodedJsonRpcContent NOTIFICATION = new EncodedJsonRpcContent( null, null );
    
	private final URI uri;
	private final Interpreter interpreter;
	private final boolean inInputPort;
	private String encoding;

	private final static int INITIAL_CAPACITY = 8;
	private final static float LOAD_FACTOR = 0.75f;
	
	private final Map< Long, String > jsonRpcIdMap;
	private final Map< String, String > jsonRpcOpMap;
	
    @Override
	public String name()
	{
		return "jsonrpc";
	}

	public JsonRpcProtocol( VariablePath configurationPath, URI uri,
				Interpreter interpreter, boolean inInputPort )
	{
		super( configurationPath );
		this.uri = uri;
		this.interpreter = interpreter;
		this.inInputPort = inInputPort;
		
		// prepare the two maps
		this.jsonRpcIdMap = new HashMap<Long, String>( INITIAL_CAPACITY, LOAD_FACTOR );
		this.jsonRpcOpMap = new HashMap<String, String>( INITIAL_CAPACITY, LOAD_FACTOR );
	}
    
    @Override
    public void setupPipeline( ChannelPipeline pipeline ){
        if( inInputPort ){
            pipeline.addLast( new HttpServerCodec() ); // TODO: could use a singleton handler?
            pipeline.addLast( new HttpContentCompressor() );
        } else {
            pipeline.addLast( new HttpClientCodec() );
            pipeline.addLast( new HttpContentDecompressor() );
        }
        pipeline.addLast( new HttpObjectAggregator( 65536 ) );
        pipeline.addLast( new JsonRpcHttpCommMessageCodec() );
        setupWrappablePipeline( pipeline );
    }
    
    @Override
    public void setupWrappablePipeline( ChannelPipeline pipeline ){
        pipeline.addLast( new JsonRpcCommMessageCodec() );
    }
    
    // EncodedJsonRpcContent <-> CommMessage
    public class JsonRpcCommMessageCodec extends MessageToMessageCodec< EncodedJsonRpcContent, CommMessage >
	{

		@Override
		protected void encode( ChannelHandlerContext ctx, CommMessage message, List<Object> out ) throws Exception
		{
			( ( CommCore.ExecutionContextThread ) Thread.currentThread() ).executionThread( 
              ctx.channel().attr( NioSocketCommChannel.EXECUTION_CONTEXT ).get() ); 
            CommMessageExt messageExt = new CommMessageExt( message );
            if( inInputPort )
                messageExt.setRequest();
            EncodedJsonRpcContent content = buildJsonRpcMessage( messageExt );
			out.add( content );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, EncodedJsonRpcContent content, List<Object> out ) throws Exception
		{
			CommMessageExt message = recv_internal( content );
			out.add( message.getCommMessage() );
		}

	}
    
    private EncodedJsonRpcContent buildJsonRpcMessage( CommMessageExt message )
		throws IOException
	{
		
		channel().setToBeClosed( !checkBooleanParameter( "keepAlive", true ) );

		if ( !message.isFault() && message.hasGenericId() && inInputPort ) {
			return NOTIFICATION;
		}
				
		Value value = Value.create();
		value.getFirstChild( "jsonrpc" ).setValue( "2.0" );
		if ( message.isFault() ) {
			Value error = value.getFirstChild( "error" );
			error.getFirstChild( "code" ).setValue( -32000 );
			error.getFirstChild( "message" ).setValue( message.fault().faultName() );
			error.getChildren( "data" ).set( 0, message.fault().value() );
			String jsonRpcId = jsonRpcIdMap.get( message.id() );
			error.getFirstChild( "id" ).setValue( jsonRpcId );
		} else {
			if ( message.isRequest() ) {
				value.getChildren( "result" ).set( 0, message.value() );
				String jsonRpcId = jsonRpcIdMap.get( message.id() );
				value.getFirstChild( "id" ).setValue( jsonRpcId );
			} else {
				jsonRpcOpMap.put( message.id() + "", message.operationName() );
				value.getFirstChild( "method" ).setValue( message.operationName() );
				if ( message.value().isDefined() || message.value().hasChildren() ) {
					// some implementations need an array here
					value.getFirstChild( "params" )
                      .getChildren( JsUtils.JSONARRAY_KEY ).set( 0, message.value() );
				}
				value.getFirstChild( "id" ).setValue( message.id() );
			}
		}
		StringBuilder json = new StringBuilder();
		JsUtils.valueToJsonString( value, true, Type.UNDEFINED, json );
		return new EncodedJsonRpcContent( 
          Unpooled.wrappedBuffer( json.toString().getBytes( "utf-8" ) ), 
          Charset.forName( "utf-8" )
        );
	}
    
     	private CommMessageExt recv_internal( EncodedJsonRpcContent content )
		throws IOException
	{
        Value value = Value.create();
		
		JsUtils.parseJsonIntoValue(
          new InputStreamReader(
            new ByteBufInputStream( content.getContent() ), 
            content.getCharset()), value, false 
        );
        
		boolean isRequest = value.hasChildren( "method" );
			
		if ( !value.hasChildren( "id" ) ) {
            
			if ( checkBooleanParameter( "debug", false ) ) {
				interpreter.logInfo( "[JSON-RPC debug] Receiving:\n" + content.text() );
			}
            
            return new CommMessageExt(
              CommMessage.GENERIC_ID, 
              value.getFirstChild( "method" ).strValue(),
              "/", 
              value.getFirstChild( "params" ), 
              null).setRequest();
		}
		
		String jsonRpcId = value.getFirstChild( "id" ).strValue();
		
        if ( isRequest ){
            jsonRpcIdMap.put( ( long ) jsonRpcId.hashCode(), jsonRpcId );
            return new CommMessageExt( 
              jsonRpcId.hashCode(), 
              value.getFirstChild( "method" ).strValue(), 
              "/", value.getFirstChild( "params" ), null).setRequest();
        } else if ( value.hasChildren( "error" ) ) {
			
            String operationName = jsonRpcOpMap.get( jsonRpcId );
            return new CommMessageExt( 
              Long.valueOf( jsonRpcId ), 
              operationName,
              "/", null,
              new FaultException(
                value.getFirstChild( "error" ).getFirstChild( "message" ).strValue(),
                value.getFirstChild( "error" ).getFirstChild( "data" )
              )
            ).setRequest();
		} else {
			// Certain implementations do not provide a result if it is "void"
			String operationName = jsonRpcOpMap.get( jsonRpcId );
			return new CommMessageExt(
              Long.valueOf( jsonRpcId ), 
              operationName, 
              "/", value.getFirstChild("result"), null).setRequest();
		}
	}
    
    // HTTP <-> EncodedJsonRpcContent
    public class JsonRpcHttpCommMessageCodec extends MessageToMessageCodec< FullHttpMessage, EncodedJsonRpcContent >
	{

		@Override
		protected void encode( ChannelHandlerContext ctx, EncodedJsonRpcContent content, List<Object> out ) throws Exception
		{
			FullHttpMessage msg = buildHttpJsonRpcMessage( content );
			out.add( msg );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, FullHttpMessage msg, List<Object> out ) throws Exception
		{
//			if ( msg instanceof FullHttpRequest ) {
//				FullHttpRequest request = (FullHttpRequest) msg;
//			} else if ( msg instanceof FullHttpResponse ) {
//				FullHttpResponse response = (FullHttpResponse) msg;
//			}
			EncodedJsonRpcContent content = recv_http_internal( msg );
			out.add( content );
		}

	}	
        
    private FullHttpMessage buildHttpJsonRpcMessage( EncodedJsonRpcContent content )
        throws IOException
    {
        FullHttpMessage httpMessage;
        
        if ( content == NOTIFICATION ){
            FullHttpResponse response = new DefaultFullHttpResponse(
              HttpVersion.HTTP_1_1, 
              HttpResponseStatus.NO_CONTENT );
            response.headers().add( HttpHeaderNames.SERVER, "Jolie" );
            return response;
        }
        
        if( inInputPort ){
            // we're responding to a request
            httpMessage = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content.getContent() );
            httpMessage.headers().add( HttpHeaderNames.SERVER, "Jolie" );
        } else {
           String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
           if ( path == null || path.length() == 0 ){
               path = "*";
           }
           httpMessage = new DefaultFullHttpRequest(
             HttpVersion.HTTP_1_1, 
             HttpMethod.POST, 
             path,
             content.getContent()
            );
           httpMessage.headers().add( HttpHeaderNames.USER_AGENT, "Jolie" );
           httpMessage.headers().add( HttpHeaderNames.HOST, uri.getHost() );
           
           if( checkBooleanParameter( "compression", true ) ){
               String requestCompression = getStringParameter( "requestCompession" );
               if ( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ){
                   encoding = requestCompression;
                   httpMessage.headers().add( HttpHeaderNames.ACCEPT_ENCODING, encoding );
               } else {
                   httpMessage.headers().add( HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate" );
               }
           }
        }
        
        if ( channel().toBeClosed() ){
            httpMessage.headers().add( HttpHeaderNames.CONNECTION, "close" );
        }
        
        httpMessage.headers().add( 
          HttpHeaderNames.CONTENT_TYPE, 
          HttpHeaderValues.APPLICATION_JSON + "; charset=utf-8" 
        );
        
        httpMessage.headers().add( 
          HttpHeaderNames.CONTENT_LENGTH, 
          httpMessage.content().readableBytes()
        );
        
        if( checkBooleanParameter( "debug", false ) ){
            interpreter.logInfo( "[JSON-RPC debug] Sending:\n" + httpMessage.toString() );
        }
        
        return httpMessage;
        
    }
    
    
    private EncodedJsonRpcContent recv_http_internal( FullHttpMessage message )
    throws IOException {
        
        Charset charset = Charset.forName( HttpUtils.getCharset( null, message ) );
        HttpUtils.recv_checkForChannelClosing( message, channel() );
        
        if ( ( message instanceof  FullHttpResponse ) && 
          ( ( FullHttpResponse ) message ).status().code() >= 400 ){
            throw new IOException( "HTTP error: " + message.content().toString( charset ) );
        }
        
        if( inInputPort && ( ( FullHttpRequest ) message ).method() != HttpMethod.POST ){
            throw new UnsupportedMethodException( "Only HTTP method POST allowed!", Method.POST );
        }
        
        encoding = message.headers().get( HttpHeaderNames.ACCEPT_ENCODING );
        
        if( message.content().readableBytes() > 0 ){
            return new EncodedJsonRpcContent( message.content().retain(), charset );
        }
        
        return null; // ERROR SITUTATION
        
    }
    
    @Override
    public boolean isThreadSafe(){
        return false;
    }

}
