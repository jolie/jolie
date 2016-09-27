/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.FileRegion;
import io.netty.channel.oio.AbstractOioByteChannel;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import javax.bluetooth.L2CAPConnection;

/**
 *
 * @author martin
 */
public class BluetoothSocketWrapper extends AbstractOioByteChannel
{
	private final L2CAPConnection l2CAPConnection;
	private final BluetoohSocketAddress address;
	private final ChannelConfig config;
	private final int sendMTU;
	private final int recvMTU;
	private boolean closed = false;
	private boolean active = false;

	public BluetoothSocketWrapper( L2CAPConnection l2CAPConnection, URI uri ) 
		throws IOException
	{
		super( null );
		this.l2CAPConnection = l2CAPConnection;
		this.address = new BluetoohSocketAddress( uri );
		this.config = new DefaultChannelConfig( this );
		this.sendMTU = l2CAPConnection.getTransmitMTU();
		this.recvMTU = l2CAPConnection.getReceiveMTU();
	}

	@Override
	protected void doConnect( SocketAddress remoteAddress, SocketAddress localAddress ) 
		throws Exception
	{
		active = true;
		closed = false;
	}

	@Override
	protected SocketAddress localAddress0()
	{
		return address;
	}

	@Override
	protected SocketAddress remoteAddress0()
	{
		return address;
	}

	@Override
	protected void doBind( SocketAddress localAddress ) throws Exception
	{
		// Do nothing
	}

	@Override
	protected void doDisconnect() throws Exception
	{
		active = false;
	}

	@Override
	protected void doClose() throws Exception
	{
		l2CAPConnection.close();
		closed = true;
	}

	public ChannelConfig config()
	{
		return config;
	}

	public boolean isOpen()
	{
		return active && !closed;
	}

	public boolean isActive()
	{
		return active;
	}

	@Override
	protected int available()
	{
		try {
		if (l2CAPConnection.ready())
			return 1;
		} catch (IOException ex) {
			return -1;
		}
		return 0;
	}

	@Override
	protected int doReadBytes( ByteBuf buf ) throws Exception
	{
		byte[] bytes = new byte[recvMTU];
		int count = l2CAPConnection.receive( bytes );
		buf.writeBytes(bytes, 0, count);
		return count;
	}

	@Override
	protected void doWriteBytes( ByteBuf buf ) throws Exception
	{		
		if ( buf.readableBytes() > sendMTU ) {
			int times = (buf.readableBytes() / sendMTU);
			int remaining = buf.readableBytes() % sendMTU;
			for( int i = 0; i < times; i++ ) {
				l2CAPConnection.send( buf.readBytes( sendMTU ).array() );
			}
			if ( remaining > 0 ) {
				l2CAPConnection.send( buf.readBytes( remaining ).array() );
			}
		} else {
			l2CAPConnection.send( buf.array() );
		}
	}

	@Override
	protected void doWriteFileRegion( FileRegion region ) throws Exception
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	@Override
	protected boolean isInputShutdown()
	{
		return false;
	}

	@Override
	protected ChannelFuture shutdownInput()
	{
		throw new UnsupportedOperationException( "Not supported." );
	}
	
	
}
