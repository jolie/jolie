/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author spz
 */
public class UdpChannel extends AbstractChannel {

    protected final ChannelMetadata metadata = new ChannelMetadata(false);
    protected final DefaultChannelConfig config = new DefaultChannelConfig(this);

    protected final UdpServerChannel serverChannel;
    protected final InetSocketAddress remote;

    protected UdpChannel(UdpServerChannel serverchannel, InetSocketAddress remote) {
        super(serverchannel);
        this.serverChannel = serverchannel;
        this.remote = remote;
    }

    protected AtomicBoolean isNew = new AtomicBoolean(true);

    protected boolean getIsNew() {
        return isNew.compareAndSet(true, false);
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
        serverChannel.doUserChannelRemove(this);
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    protected final ConcurrentLinkedQueue<ByteBuf> buffers = new ConcurrentLinkedQueue<>();

    protected void addBuffer(ByteBuf buffer) {
        this.buffers.add(buffer);
    }

    protected boolean reading = false;

    @Override
    protected void doBeginRead() throws Exception {
        if (reading) {
            return;
        }
        reading = true;
        try {
            ByteBuf buffer = null;
            while ((buffer = buffers.poll()) != null) {
                pipeline().fireChannelRead(buffer);
            }
            pipeline().fireChannelReadComplete();
        } finally {
            reading = false;
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer buffer) throws Exception {
        final RecyclableArrayList list = RecyclableArrayList.newInstance();
        boolean freeList = true;
        try {
            ByteBuf buf = null;
            while ((buf = (ByteBuf) buffer.current()) != null) {
                list.add(buf.retain());
                buffer.remove();
            }
            freeList = false;
        } finally {
            if (freeList) {
                list.forEach((obj) -> {
                    ReferenceCountUtil.safeRelease(obj);
                });
                list.recycle();
            }
        }
        serverChannel.doWrite(list, remote);
    }

    @Override
    protected boolean isCompatible(EventLoop eventloop) {
        return true;
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress addr1, SocketAddress addr2, ChannelPromise pr) {
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
    protected void doBind(SocketAddress addr) throws Exception {
        throw new UnsupportedOperationException();
    }

}
