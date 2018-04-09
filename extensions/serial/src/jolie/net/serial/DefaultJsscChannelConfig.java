package jolie.net.serial;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;

import java.util.Map;
import static jolie.net.serial.JsscChannelOption.*;

/**
 * Default configuration class for JSSC device connections.
 */
final class DefaultJsscChannelConfig extends DefaultChannelConfig implements JsscChannelConfig {

	private volatile int baudrate = 9600;
	private volatile boolean dtr;
	private volatile boolean rts;
	private volatile Integer stopbits = 1;
	private volatile Integer databits = 8;
	private volatile Integer paritybit = 0;
	private volatile Integer waitTime = 0;

	public DefaultJsscChannelConfig( JsscChannel channel ) {
		super( channel );
	}

	@Override
	public Map<ChannelOption<?>, Object> getOptions() {
		return getOptions( super.getOptions(), BAUD_RATE, DTR, RTS, STOP_BITS, DATA_BITS, PARITY_BIT, WAIT_TIME );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> T getOption( ChannelOption<T> option ) {
		if ( option == BAUD_RATE ) {
			return ( T ) getBaudrate();
		}
		if ( option == DTR ) {
			return ( T ) isDtr();
		}
		if ( option == RTS ) {
			return ( T ) isRts();
		}
		if ( option == STOP_BITS ) {
			return ( T ) getStopbits();
		}
		if ( option == DATA_BITS ) {
			return ( T ) getDatabits();
		}
		if ( option == PARITY_BIT ) {
			return ( T ) getParitybit();
		}
		if ( option == WAIT_TIME ) {
			return ( T ) getWaitTimeMillis();
		}
		return super.getOption( option );
	}

	@Override
	public <T> boolean setOption( ChannelOption<T> option, T value ) {
		validate( option, value );

		if ( option == BAUD_RATE ) {
			setBaudrate( ( Integer ) value );
		} else if ( option == DTR ) {
			setDtr( ( Boolean ) value );
		} else if ( option == RTS ) {
			setRts( ( Boolean ) value );
		} else if ( option == STOP_BITS ) {
			setStopbits( ( Integer ) value );
		} else if ( option == DATA_BITS ) {
			setDatabits( ( Integer ) value );
		} else if ( option == PARITY_BIT ) {
			setParitybit( ( Integer ) value );
		} else if ( option == WAIT_TIME ) {
			setWaitTimeMillis( ( Integer ) value );
		} else {
			return super.setOption( option, value );
		}
		return true;
	}

	@Override
	public JsscChannelConfig setBaudrate( Integer baudrate ) {
		this.baudrate = baudrate;
		return this;
	}

	@Override
	public JsscChannelConfig setStopbits( Integer stopbits ) {
		this.stopbits = stopbits;
		return this;
	}

	@Override
	public JsscChannelConfig setDatabits( Integer databits ) {
		this.databits = databits;
		return this;
	}

	@Override
	public JsscChannelConfig setParitybit( Integer paritybit ) {
		this.paritybit = paritybit;
		return this;
	}

	@Override
	public Integer getBaudrate() {
		return baudrate;
	}

	@Override
	public Integer getStopbits() {
		return stopbits;
	}

	@Override
	public Integer getDatabits() {
		return databits;
	}

	@Override
	public Integer getParitybit() {
		return paritybit;
	}

	@Override
	public Boolean isDtr() {
		return dtr;
	}

	@Override
	public JsscChannelConfig setDtr( final boolean dtr ) {
		this.dtr = dtr;
		return this;
	}

	@Override
	public Boolean isRts() {
		return rts;
	}

	@Override
	public JsscChannelConfig setRts( final boolean rts ) {
		this.rts = rts;
		return this;
	}

	@Override
	public Integer getWaitTimeMillis() {
		return waitTime;
	}

	@Override
	public JsscChannelConfig setWaitTimeMillis( final int waitTimeMillis ) {
		if ( waitTimeMillis < 0 ) {
			throw new IllegalArgumentException( "Wait time must be >= 0" );
		}
		waitTime = waitTimeMillis;
		return this;
	}

	@Override
	public JsscChannelConfig setConnectTimeoutMillis( int connectTimeoutMillis ) {
		super.setConnectTimeoutMillis( connectTimeoutMillis );
		return this;
	}

	@Override
	public JsscChannelConfig setMaxMessagesPerRead( int maxMessagesPerRead ) {
		super.setMaxMessagesPerRead( maxMessagesPerRead );
		return this;
	}

	@Override
	public JsscChannelConfig setWriteSpinCount( int writeSpinCount ) {
		super.setWriteSpinCount( writeSpinCount );
		return this;
	}

	@Override
	public JsscChannelConfig setAllocator( ByteBufAllocator allocator ) {
		super.setAllocator( allocator );
		return this;
	}

	@Override
	public JsscChannelConfig setRecvByteBufAllocator( RecvByteBufAllocator allocator ) {
		super.setRecvByteBufAllocator( allocator );
		return this;
	}

	@Override
	public JsscChannelConfig setAutoRead( boolean autoRead ) {
		super.setAutoRead( autoRead );
		return this;
	}

	@Override
	public JsscChannelConfig setAutoClose( boolean autoClose ) {
		super.setAutoClose( autoClose );
		return this;
	}

	@Override
	public JsscChannelConfig setWriteBufferHighWaterMark( int writeBufferHighWaterMark ) {
		super.setWriteBufferHighWaterMark( writeBufferHighWaterMark );
		return this;
	}

	@Override
	public JsscChannelConfig setWriteBufferLowWaterMark( int writeBufferLowWaterMark ) {
		super.setWriteBufferLowWaterMark( writeBufferLowWaterMark );
		return this;
	}

	@Override
	public JsscChannelConfig setMessageSizeEstimator( MessageSizeEstimator estimator ) {
		super.setMessageSizeEstimator( estimator );
		return this;
	}
}
