/*
 * The MIT License
 *
 * Copyright 2018 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jolie.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValuePrettyPrinter;

public class JSerialCommClientHandler extends MessageToMessageCodec<String, CommMessage> {

	private final boolean isInput;

	public JSerialCommClientHandler( boolean isInput ) {
		this.isInput = isInput;
	}

	@Override
	protected void encode( ChannelHandlerContext chc, CommMessage otbndn, List<Object> list ) throws Exception {
		if ( isInput ) {
			String out = otbndn.value().strValue();
			System.out.println( "value: " + out );
			list.add( out );
		} else {
		}
	}

	@Override
	protected void decode( ChannelHandlerContext chc, String inbndn, List<Object> list ) throws Exception {
		if ( isInput ) {
			long id = CommMessage.getNewMessageId();
			String operationName = "sendAndReceive";
			String resourcePath = "/";
			Value value = Value.create( inbndn );
			System.out.println( valueToPrettyString( value ));
			FaultException fault = null;
			CommMessage out = new CommMessage( id, operationName, resourcePath, value, fault );
			list.add( out );
		} else {

		}
	}

	public String valueToPrettyString( Value request ) {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( request, writer, "Value" );
		try {
			printer.run();
		} catch ( IOException e ) {
		} // Should never happen
		return writer.toString();
	}
}
