package jolie.net.jssc;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;

/**
 * A configuration class for JSSC device connections.
 *
 * <h3>Available options</h3>
 *
 * In addition to the options provided by {@link ChannelConfig},
 * {@link DefaultJsscChannelConfig} allows the following options in the option map:
 *
 * <table border="1" cellspacing="0" cellpadding="6" summary="">
 * <tr>
 * <th>Name</th><th>Associated setter method</th>
 * </tr><tr>
 * <td>{@link JsscChannelOption#BAUD_RATE}</td><td>{@link #setBaudrate(Integer)}</td>
 * </tr><tr>
 * <td>{@link JsscChannelOption#DTR}</td><td>{@link #setDtr(boolean)}</td>
 * </tr><tr>
 * <td>{@link JsscChannelOption#RTS}</td><td>{@link #setRts(boolean)}</td>
 * </tr><tr>
 * <td>{@link JsscChannelOption#STOP_BITS}</td><td>{@link #setStopbits(Integer)}</td>
 * </tr><tr>
 * <td>{@link JsscChannelOption#DATA_BITS}</td><td>{@link #setDatabits(Integer)}</td>
 * </tr><tr>
 * <td>{@link JsscChannelOption#PARITY_BIT}</td><td>{@link #setParitybit(Integer)}</td>
 * </tr>
 * </table>
 */
public interface JsscChannelConfig extends ChannelConfig {
    /**
     * Sets the baud rate (ie. bits per second) for communication with the serial device.
     * The baud rate will include bits for framing (in the form of stop bits and parity),
     * such that the effective data rate will be lower than this value.
     *
     * @param baudrate The baud rate (in bits per second)
     */
    JsscChannelConfig setBaudrate(Integer baudrate);

    /**
     * Sets the number of stop bits to include at the end of every character to aid the
     * serial device in synchronising with the data.
     *
     * @param stopbits The number of stop bits to use
     * @return the channel config
     */
    JsscChannelConfig setStopbits(Integer stopbits);

    /**
     * Sets the number of data bits to use to make up each character sent to the serial
     * device.
     *
     * @param databits The number of data bits to use
     * @return the channel config
     */
    JsscChannelConfig setDatabits(Integer databits);

    /**
     * Sets the type of parity bit to be used when communicating with the serial device.
     *
     * @param paritybit The type of parity bit to be used
     * @return the channel config
     */
    JsscChannelConfig setParitybit(Integer paritybit);

    /**
     * @return The configured baud rate, defaulting to 115200 if unset
     */
    Integer getBaudrate();

    /**
     * @return The configured stop bits
     */
    Integer getStopbits();

    /**
     * @return The configured data bits
     */
    Integer getDatabits();

    /**
     * @return The configured parity bit
     */
    Integer getParitybit();

    /**
     * @return true if the serial device should support the Data Terminal Ready signal
     */
    Boolean isDtr();

    /**
     * Sets whether the serial device supports the Data Terminal Ready signal, used for
     * flow control
     *
     * @param dtr true if DTR is supported, false otherwise
     * @return the channel config
     */
    JsscChannelConfig setDtr(boolean dtr);

    /**
     * @return true if the serial device should support the Ready to Send signal
     */
    Boolean isRts();

    /**
     * Sets whether the serial device supports the Request To Send signal, used for flow
     * control
     *
     * @param rts true if RTS is supported, false otherwise
     * @return the channel config
     */
    JsscChannelConfig setRts(boolean rts);

    /**
     * @return The number of milliseconds to wait between opening the serial port and
     *     initialising.
     */
    Integer getWaitTimeMillis();

    /**
     * Sets the time to wait after opening the serial port and before sending it any
     * configuration information or data. A value of 0 indicates that no waiting should
     * occur.
     *
     * @param waitTimeMillis The number of milliseconds to wait, defaulting to 0 (no
     *     wait) if unset
     * @return the channel config
     */
    JsscChannelConfig setWaitTimeMillis(int waitTimeMillis);

    @Override
    JsscChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    JsscChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    JsscChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    JsscChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    JsscChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    JsscChannelConfig setAutoRead(boolean autoRead);

    @Override
    JsscChannelConfig setAutoClose(boolean autoClose);

    @Override
    JsscChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark);

    @Override
    JsscChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark);

    @Override
    JsscChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);
}
