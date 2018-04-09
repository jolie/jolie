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

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

public class ModbusProtocol extends AsyncCommProtocol {

	final boolean isInput;

	public ModbusProtocol( VariablePath configurationPath, boolean isInput ) {
		super( configurationPath );
		this.isInput = isInput;
	}

	@Override
	public void setupPipeline( ChannelPipeline pipeline ) {
		pipeline.addLast( "logger", new LoggingHandler( LogLevel.INFO ) );
		pipeline.addLast( "frameDecoder", new LineBasedFrameDecoder( 32768 ) );
		pipeline.addLast( "stringDecoder", new StringDecoder( CharsetUtil.UTF_8 ) );
		pipeline.addLast( "strinEncoder", new StringEncoder( CharsetUtil.UTF_8 ) );
		pipeline.addLast( "modbusCodec", new ModbusToCommMessageCodec() );
	}

	@Override
	public String name() {
		return "modbus";
	}

	@Override
	public boolean isThreadSafe() {
		return false;
	}

}
