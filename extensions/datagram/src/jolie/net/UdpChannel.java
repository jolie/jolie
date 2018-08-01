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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;

public class UdpChannel extends AbstractChannel {

	protected final ChannelMetadata metadata = new ChannelMetadata( false );
	protected final DefaultChannelConfig config = new DefaultChannelConfig( this );

	protected final UdpServerChannel serverChannel;
	protected final InetSocketAddress remote;

	protected UdpChannel( UdpServerChannel serverchannel, InetSocketAddress remote ) {
		super( serverchannel );
		this.serverChannel = serverchannel;
		this.remote = remote;
	}

	protected AtomicBoolean isNew = new AtomicBoolean( true );

	protected boolean getIsNew() {
		return isNew.compareAndSet( true, false );
	}

	@Override
	public ChannelMetadata metadata() {
		return metadata;
	}

	@Override
	public ChannelConfig config() {
		return config;
	}

	protected volatile boolean open = true;

	@Override
	public boolean isActive() {
		return open;
	}

	@Override
	public boolean isOpen() {
		return isActive();
	}

	@Override
	protected void doClose() throws Exception {
		open = false;
		serverChannel.doUserChannelRemove( this );
	}

	@Override
	protected void doDisconnect() throws Exception {
		doClose();
	}

	protected final ConcurrentLinkedQueue<ByteBuf> buffers = new ConcurrentLinkedQueue<>();

	protected void addBuffer( ByteBuf buffer ) {
		this.buffers.add( buffer );
	}

	protected boolean reading = false;

	@Override
	protected void doBeginRead() throws Exception {
		if ( reading ) {
			return;
		}
		reading = true;
		try {
			ByteBuf buffer = null;
			while ( ( buffer = buffers.poll() ) != null ) {
				pipeline().fireChannelRead( buffer );
			}
			pipeline().fireChannelReadComplete();
		} finally {
			reading = false;
		}
	}

	@Override
	protected void doWrite( ChannelOutboundBuffer buffer ) throws Exception {
		final RecyclableArrayList list = RecyclableArrayList.newInstance();
		boolean freeList = true;
		try {
			ByteBuf buf = null;
			while ( ( buf = ( ByteBuf ) buffer.current() ) != null ) {
				list.add( buf.retain() );
				buffer.remove();
			}
			freeList = false;
		} finally {
			if ( freeList ) {
				for ( Object obj : list ) {
					ReferenceCountUtil.safeRelease( obj );
				}
				list.recycle();
			}
		}
		serverChannel.doWrite( list, remote );
	}

	@Override
	protected boolean isCompatible( EventLoop eventloop ) {
		return true;
	}

	@Override
	protected AbstractUnsafe newUnsafe() {
		return new AbstractUnsafe() {
			@Override
			public void connect( SocketAddress addr1, SocketAddress addr2, ChannelPromise pr ) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	protected SocketAddress localAddress0() {
		return serverChannel.localAddress0();
	}

	@Override
	protected SocketAddress remoteAddress0() {
		return remote;
	}

	@Override
	protected void doBind( SocketAddress addr ) throws Exception {
		throw new UnsupportedOperationException();
	}

}
