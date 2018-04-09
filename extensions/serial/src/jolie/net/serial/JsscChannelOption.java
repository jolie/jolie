package jolie.net.serial;

import io.netty.channel.ChannelOption;

/**
 * Option for configuring a serial port connection
 */
public final class JsscChannelOption<T> extends ChannelOption<T> {

	public static final JsscChannelOption<Integer> BAUD_RATE
		= new JsscChannelOption<Integer>( "BAUD_RATE" );

	public static final JsscChannelOption<Boolean> DTR
		= new JsscChannelOption<Boolean>( "DTR" );

	public static final JsscChannelOption<Boolean> RTS
		= new JsscChannelOption<Boolean>( "RTS" );

	public static final JsscChannelOption<Integer> STOP_BITS
		= new JsscChannelOption<Integer>( "STOP_BITS" );

	public static final JsscChannelOption<Integer> DATA_BITS
		= new JsscChannelOption<Integer>( "DATA_BITS" );

	public static final JsscChannelOption<Integer> PARITY_BIT
		= new JsscChannelOption<Integer>( "PARITY_BIT" );

	public static final JsscChannelOption<Integer> WAIT_TIME
		= new JsscChannelOption<Integer>( "WAIT_TIME" );

	@SuppressWarnings( "deprecation" )
	private JsscChannelOption( String name ) {
		super( name );
	}
}
