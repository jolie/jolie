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
package jolie.net.coap.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import jolie.Interpreter;
import jolie.net.coap.communication.blockwise.BlockSize;
import jolie.net.coap.message.options.ContentFormat;
import jolie.net.coap.message.options.EmptyOptionValue;
import jolie.net.coap.message.options.OpaqueOptionValue;
import jolie.net.coap.message.options.Option;
import jolie.net.coap.message.options.OptionValue;
import jolie.net.coap.message.options.StringOptionValue;
import jolie.net.coap.message.options.UintOptionValue;

/**
 * This class is the base class for inheriting subtypes, e.g. requests and
 * responses. This abstract class provides the cut-set in terms of functionality
 * of {@link CoapRequest} and {@link CoapResponse}.
 *
 * @author Oliver Kleine
 */
public class CoapMessage
{

	public static final int PROTOCOL_VERSION = 1;
	public static final Charset CHARSET = CharsetUtil.UTF_8;
	public static final int UNDEFINED_MESSAGE_ID = -1;
	public static final int MAX_TOKEN_LENGTH = 8;
	private static final String WRONG_OPTION_TYPE = "Option no. %d is no option of type %s";
	private static final String OPTION_NOT_ALLOWED_WITH_MESSAGE_TYPE = "Option no. %d (%s) is not allowed with "
		+ "message type %s";
	private static final String OPTION_ALREADY_SET = "Option no. %d is already set and is only allowed once per "
		+ "message";
	private static final String DOES_NOT_ALLOW_CONTENT = "CoAP messages with code %s do not allow payload.";
	private static final String EXCLUDES = "Already contained option no. %d excludes option no. %d";

	private int messageType;
	private int messageCode;
	private int messageId;
	private Token token;

	private ByteBuf content;

	protected Map<Integer, List<OptionValue>> options;

	/**
	 * Creates a new instance of {@link CoapMessage}.
	 *
	 * @param messageType the number representing the {@link MessageType} for this
	 * {@link CoapMessage}
	 * @param messageCode the number representing the {@link MessageCode} for this
	 * {@link CoapMessage}
	 * @param messageID the message ID for this {@link CoapMessage}
	 * @param token the {@link Token} for this {@link CoapMessage}
	 *
	 * @throws IllegalArgumentException if one of the given arguments is invalid
	 */
	public CoapMessage( int messageType, int messageCode, int messageID,
		Token token ) throws IllegalArgumentException
	{

		if ( !MessageType.isValidMessageType( messageType ) ) {
			Interpreter.getInstance().logSevere( "No. " + messageType
				+ " is not corresponding to any message type." );
		}

		if ( !MessageCode.isValidMessageCode( messageCode ) ) {
			Interpreter.getInstance().logSevere( "No. " + messageCode
				+ " is not corresponding to any message code." );
		}

		this.messageType( messageType );
		this.setMessageCode( messageCode );

		this.id( messageID );
		this.token( token );

		this.options = new HashMap<>();
		this.content = Unpooled.EMPTY_BUFFER;
	}

	/**
	 * Creates a new instance of {@link CoapMessage}. Invocation of this
	 * constructor has the same effect as invocation of
	 * {@link #CoapMessage(int, int, int, Token)} with
	 * <ul>
	 * <li>
	 * message ID: {@link CoapMessage#UNDEFINED_MESSAGE_ID} (to be set
	 * automatically by the framework)
	 * </li>
	 * <li>
	 * token: {@link Token#Token(byte[])} with empty byte array.
	 * </li>
	 * </ul>
	 *
	 * @param messageType the number representing the {@link MessageType} for this
	 * {@link CoapMessage}
	 * @param messageCode the number representing the {@link MessageCode} for this
	 * {@link CoapMessage}
	 *
	 * @throws IllegalArgumentException if one of the given arguments is invalid
	 */
	protected CoapMessage( int messageType, int messageCode ) throws IllegalArgumentException
	{
		this( messageType, messageCode, UNDEFINED_MESSAGE_ID, new Token( new byte[ 0 ] ) );
	}

	/**
	 * Method to create an empty reset message which is strictly speaking neither
	 * a request nor a response
	 *
	 * @param messageID the message ID of the reset message.
	 *
	 * @return an instance of {@link CoapMessage} with {@link MessageType#RST}
	 *
	 * @throws IllegalArgumentException if the given message ID is out of the
	 * allowed range
	 */
	public static CoapMessage createEmptyReset( int messageID )
		throws IllegalArgumentException
	{
		return new CoapMessage( MessageType.RST, MessageCode.EMPTY, messageID,
			new Token( new byte[ 0 ] ) )
		{
		};
	}

	/**
	 * Method to create an empty acknowledgement message which is strictly
	 * speaking neither a request nor a response
	 *
	 * @param id the message ID of the acknowledgement message.
	 *
	 * @return an instance of {@link CoapMessage} with {@link MessageType#ACK}
	 *
	 * @throws IllegalArgumentException if the given message ID is out of the
	 * allowed range
	 */
	public static CoapMessage createEmptyAcknowledgement( int id )
		throws IllegalArgumentException
	{
		return new CoapMessage( MessageType.ACK, MessageCode.EMPTY, id,
			new Token( new byte[ 0 ] ) )
		{
		};
	}

	/**
	 * Method to create an empty confirmable message which is considered a PIMG
	 * message on application layer, i.e. a message to check if a CoAP endpoints
	 * is alive (not only the host but also the CoAP application!).
	 *
	 * @param messageID the message ID of the acknowledgement message.
	 *
	 * @return an instance of {@link CoapMessage} with {@link MessageType#CON}
	 *
	 * @throws IllegalArgumentException if the given message ID is out of the
	 * allowed range
	 */
	public static CoapMessage createPing( int id )
		throws IllegalArgumentException
	{
		return new CoapMessage( MessageType.CON, MessageCode.EMPTY, id,
			new Token( new byte[ 0 ] ) );
	}

	/**
	 * Sets the message type of this {@link CoapMessage}. Usually there is no need
	 * to use this method as the value is either set via constructor parameter
	 * (for requests) or automatically by the nCoAP framework (for responses).
	 *
	 * @param messageType the number representing the message type of this method
	 *
	 * @throws java.lang.IllegalArgumentException if the given message type is not
	 * supported.
	 */
	public void messageType( int messageType )
		throws IllegalArgumentException
	{
		if ( !MessageType.isValidMessageType( messageType ) ) {
			throw new IllegalArgumentException( "Invalid message type ("
				+ messageType
				+ "). Only numbers 0-3 are allowed." );
		}

		this.messageType = messageType;
	}

	public boolean isPing()
	{
		return this.messageCode == MessageCode.EMPTY
			&& this.messageType == MessageType.CON;
	}

	public boolean isRequest()
	{
		return MessageCode.isRequest( this.messageCode() );
	}

	public boolean isResponse()
	{
		return MessageCode.isResponse( this.messageCode() );
	}

	/**
	Check if the message is and Acknowledgment one.
	@return true if this message is an { @link MessageType.ACK }, false otherwise
	 */
	public boolean isAck()
	{
		return this.messageType == MessageType.ACK;
	}

	/**
	Check if the message is and Empty Acknowledgment one.
	@return true if this message is an { @link MessageType.ACK } and { @link MessageCode.EMPTY }, false otherwise
	 */
	public boolean isEmptyAck()
	{
		return this.messageType == MessageType.ACK && this.messageCode == MessageCode.EMPTY;
	}

	/**
	 * Adds an option to this {@link CoapMessage}. However, it is recommended to
	 * use the options specific methods from {@link CoapRequest} and
	 * {@link CoapResponse} to add options. This method is intended for framework
	 * internal use.
	 *
	 * @param optionNumber the number representing the option type
	 * @param optionValue the {@link OptionValue} of this option
	 *
	 * @throws java.lang.IllegalArgumentException if the given option number is
	 * unknwon, or if the given value is either the default value or exceeds the
	 * defined length limits for options with the given option number
	 */
	public void addOption( int optionNumber, OptionValue optionValue ) throws IllegalArgumentException
	{
		this.checkOptionPermission( optionNumber );

		for( int containedOption : options.keySet() ) {
			if ( Option.mutuallyExcludes( containedOption, optionNumber ) ) {
				throw new IllegalArgumentException( String.format( EXCLUDES, containedOption, optionNumber ) );
			}
		}
		switch( Option.getPermittedOccurence( optionNumber, this.messageCode ) ) {
			case NONE:
				break;
			case ONCE:
				options.putIfAbsent( optionNumber, Collections.singletonList( optionValue ) );
				break;
			case MULTIPLE:
				if ( options.get( optionNumber ) == null ) {
					List<OptionValue> optionValueList = new ArrayList<>();
					optionValueList.add( optionValue );
					options.put( optionNumber, optionValueList );
				} else {
					options.get( optionNumber ).add( optionValue );
				}
				break;
			default:
				throw new AssertionError( Option.getPermittedOccurence( optionNumber, this.messageCode ).name() );
		}
	}

	/**
	 * Adds an string option to this {@link CoapMessage}. However, it is
	 * recommended to use the options specific methods from {@link CoapRequest}
	 * and {@link CoapResponse} to add options. This method is intended for
	 * framework internal use.
	 *
	 * @param optionNumber the number representing the option type
	 * @param value the value of this string option
	 *
	 * @throws java.lang.IllegalArgumentException if the given option number
	 * refers to an unknown option or if the given {@link OptionValue} is not
	 * valid, e.g. to long
	 */
	public void addStringOption( int optionNumber, String value )
		throws IllegalArgumentException
	{

		if ( !(OptionValue.getType( optionNumber ) == OptionValue.Type.STRING) ) {
			throw new IllegalArgumentException( String.format( WRONG_OPTION_TYPE,
				optionNumber, OptionValue.Type.STRING ) );
		}

		addOption( optionNumber, new StringOptionValue( optionNumber, value ) );
	}

	/**
	 * Adds an uint option to this {@link CoapMessage}. However, it is recommended
	 * to use the options specific methods from {@link CoapRequest} and
	 * {@link CoapResponse} to add options. This method is intended for framework
	 * internal use.
	 *
	 * @param optionNumber the number representing the option type
	 * @param value the value of this uint option
	 *
	 * @throws java.lang.IllegalArgumentException
	 */
	public void addUintOption( int optionNumber, long value )
		throws IllegalArgumentException
	{

		if ( !(OptionValue.getType( optionNumber ) == OptionValue.Type.UINT) ) {
			throw new IllegalArgumentException( String.format( WRONG_OPTION_TYPE,
				optionNumber, OptionValue.Type.STRING ) );
		}
		byte[] byteValue = ByteBuffer.allocate( Long.BYTES ).putLong( value ).array();
		int index = 0;
		while( index < byteValue.length && byteValue[ index ] == 0 ) {
			index++;
		}
		addOption( optionNumber, new UintOptionValue( optionNumber,
			Arrays.copyOfRange( byteValue, index, byteValue.length ) ) );
	}

	/**
	 * Adds an opaque option to this {@link CoapMessage}. However, it is
	 * recommended to use the options specific methods from {@link CoapRequest}
	 * and {@link CoapResponse} to add options. This method is intended for
	 * framework internal use.
	 *
	 * @param optionNumber the number representing the option type
	 * @param value the value of this opaque option
	 *
	 * @throws java.lang.IllegalArgumentException
	 */
	public void addOpaqueOption( int optionNumber, byte[] value )
		throws IllegalArgumentException
	{

		if ( !(OptionValue.getType( optionNumber ) == OptionValue.Type.OPAQUE) ) {
			throw new IllegalArgumentException( String.format( WRONG_OPTION_TYPE,
				optionNumber, OptionValue.Type.OPAQUE ) );
		}

		addOption( optionNumber, new OpaqueOptionValue( optionNumber, value ) );
	}

	/**
	 * Adds an empty option to this {@link CoapMessage}. However, it is
	 * recommended to use the options specific methods from {@link CoapRequest}
	 * and {@link CoapResponse} to add options. This method is intended for
	 * framework internal use.
	 *
	 * @param optionNumber the number representing the option type
	 *
	 * @throws java.lang.IllegalArgumentException if the given option number
	 * refers to an unknown option or to a not-empty option.
	 */
	public void addEmptyOption( int optionNumber )
		throws IllegalArgumentException
	{

		if ( !(OptionValue.getType( optionNumber ) == OptionValue.Type.EMPTY) ) {
			throw new IllegalArgumentException( String.format( WRONG_OPTION_TYPE,
				optionNumber, OptionValue.Type.EMPTY ) );
		}
		addOption( optionNumber, new EmptyOptionValue( optionNumber ) );
	}

	/**
	 * Removes all options with the given option number from this
	 * {@link CoapMessage} instance.
	 *
	 * @param optionNumber the option number to remove from this message
	 *
	 * @return the number of options that were removed, i.e. the count.
	 */
	public int removeOptions( int optionNumber )
	{
		this.options.remove( optionNumber );
		int result = options.size();
		return result;
	}

	/**
	 * Returns the CoAP protocol version used for this {@link CoapMessage}
	 *
	 * @return the CoAP protocol version used for this {@link CoapMessage}
	 */
	public int getProtocolVersion()
	{
		return PROTOCOL_VERSION;
	}

	/**
	 * Sets a Random geenerated message ID for this message. However, there is no
	 * need to set the message ID manually. It is set (or overwritten)
	 * automatically by the nCoAP framework.
	 *
	 */
	public void randomId()
	{
		this.id( new Random().nextInt( 65535 ) );
	}

	/**
	 * Sets the message ID for this message. However, there is no need to set the
	 * message ID manually. It is set (or overwritten) automatically by the nCoAP
	 * framework.
	 *
	 * @param messageID the message ID for the message
	 */
	public void id( int messageID ) throws IllegalArgumentException
	{

		if ( messageID < -1 || messageID > 65535 ) {
			throw new IllegalArgumentException( "Message ID "
				+ messageID + " is either negative or greater than 65535" );
		}

		this.messageId = messageID;
	}

	/**
	 * Returns the message ID (or {@link CoapMessage#UNDEFINED_MESSAGE_ID} if not
	 * set)
	 *
	 * @return the message ID (or {@link CoapMessage#UNDEFINED_MESSAGE_ID} if not
	 * set)
	 */
	public int id()
	{
		return this.messageId;
	}

	/**
	 * Returns the number representing the {@link MessageType} of this
	 * {@link CoapMessage}
	 *
	 * @return the number representing the {@link MessageType} of this
	 * {@link CoapMessage}
	 */
	public int messageType()
	{
		return this.messageType;
	}

	/**
	 * Returns the {@link java.lang.String} representation of this
	 * {@link CoapMessage}s type
	 *
	 * @return the {@link java.lang.String} representation of this
	 * {@link CoapMessage}s type
	 */
	public String getMessageTypeName()
	{
		return MessageType.asString( this.messageType );
	}

	/**
	 * Returns the number representing the {@link MessageCode} of this
	 * {@link CoapMessage}
	 *
	 * @return the number representing the {@link MessageCode} of this
	 * {@link CoapMessage}
	 */
	public int messageCode()
	{
		return this.messageCode;
	}

	/**
	 * Returns the {@link java.lang.String} representation of this
	 * {@link CoapMessage}s code
	 *
	 * @return the {@link java.lang.String} representation of this
	 * {@link CoapMessage}s code
	 */
	public String getMessageCodeName()
	{
		return MessageCode.asString( this.messageCode );
	}

	/**
	 * Sets a {@link Token} to this {@link CoapMessage}. However, there is no need
	 * to set the {@link Token} manually, as it is set (or overwritten)
	 * automatically by the framework.
	 *
	 * @param token the {@link Token} for this {@link CoapMessage}
	 */
	public void token( Token token )
	{
		this.token = token;
	}

	/**
	 * Sets a random generated {@link Token} to this {@link CoapMessage} with
	 * maximum length. However, there is no need to set the {@link Token}
	 * manually, as it is set (or overwritten) automatically by the framework.
	 *
	 */
	public void setRandomToken()
	{
		this.token = Token.getRandomToken();
	}

	/**
	 * Returns the {@link Token} of this {@link CoapMessage}
	 *
	 * @return the {@link Token} of this {@link CoapMessage}
	 */
	public Token token()
	{
		return this.token;
	}

	/**
	 * Sets the observing option in this {@link CoapRequest} and returns
	 * <code>true</code> if the option is set after method returns (may already
	 * have been set beforehand in a prior method invocation) or
	 * <code>false</code> if the option is not set, e.g. because that option has
	 * no meaning with the message code of this {@link CoapRequest}.
	 *
	 * @param value <code>true</code> if this {@link CoapRequest} is supposed to
	 * register as an observer and <code>false</code> to deregister as observer,
	 * i.e. cancel an ongoing observation
	 */
	public void setObserve( long value )
	{
		try {
			this.removeOptions( Option.OBSERVE );
			value = value & 0xFFFFFF;
			this.addUintOption( Option.OBSERVE, value );
		} catch( IllegalArgumentException e ) {
			this.removeOptions( Option.OBSERVE );
			Interpreter.getInstance().logInfo( "This should never happen." + e );
		}
	}

	/**
	 * Returns the value of the observing option (no.6) or
	 * {@link UintOptionValue#UNDEFINED} if there is no such option present in
	 * this {@link CoapRequest}.
	 *
	 * @return he value of the observing option (no.6) or
	 * {@link UintOptionValue#UNDEFINED} if there is no such option present in
	 * this {@link CoapRequest}.
	 */
	public long getObserve()
	{
		if ( options.containsKey( Option.OBSERVE ) ) {
			return (long) options.get( Option.OBSERVE ).get( 0 ).getDecodedValue();
		} else {
			return UintOptionValue.UNDEFINED;
		}
	}

	/**
	 * Returns the sequence number of the block2 option or
	 * {@link jolie.net.coap.message.options.UintOptionValue#UNDEFINED} if there
	 * is no such option present in this {@link CoapRequest}.
	 *
	 * @return the sequence number of the block2 option or
	 * {@link jolie.net.coap.message.options.UintOptionValue#UNDEFINED} if there
	 * is no such option present in this {@link CoapRequest}.
	 */
	public long getBlock2Number()
	{
		if ( !options.containsKey( Option.BLOCK_2 ) ) {
			return UintOptionValue.UNDEFINED;
		} else {
			return (long) options.get( Option.BLOCK_2 ).iterator().next().getDecodedValue() >> 4;
		}
	}

	/**
	 * Returns <code>true</code> if
	 * <ul>
	 * <li>the BLOCK2 option is present and its value indicates that there are no
	 * more blocks to come (should be always <code>false</code> for
	 * {@link CoapRequest}s or
	 * </li>
	 * <li>if there is no BLOCK2 option present.</li>
	 * </ul>
	 *
	 * @return <code>true</code> if there are no more blocks expected.
	 */
	public boolean isLastBlock2()
	{
		if ( !options.containsKey( Option.BLOCK_2 ) ) {
			return true;
		} else {
			long m = (long) options.get( Option.BLOCK_2 ).iterator().next().getDecodedValue();
			return (extractBits( m, 1, 3 ) == 0);
		}
	}

	/**
	 * Returns encoded block size of the block2 option (i.e. the 'szx' portion) or
	 * {@link de.uzl.itm.ncoap.message.options.UintOptionValue#UNDEFINED} if there
	 * is no such option present in this {@link CoapRequest}.
	 *
	 * With szx as the returned value the actual blocksize is
	 * <code>2^(szx + 4)</code> bytes.
	 *
	 * @return the block size of the block2 option or
	 * {@link de.uzl.itm.ncoap.message.options.UintOptionValue#UNDEFINED} if there
	 * is no such option present in this {@link CoapRequest}.
	 */
	public long getBlock2Szx()
	{
		if ( !options.containsKey( Option.BLOCK_2 ) ) {
			return UintOptionValue.UNDEFINED;
		} else {
			long value = (long) options.get( Option.BLOCK_2 ).iterator().next().getDecodedValue();
			return extractBits( value, 3, 0 );
		}
	}

	public long getBlock2Size()
	{
		long block2szx = getBlock2Szx();
		if ( block2szx != BlockSize.UNDEFINED ) {
			return BlockSize.UNDEFINED;
		} else {
			return BlockSize.getBlockSize( block2szx ).getSize();
		}
	}

	/**
	 * Returns the sequence number (i.e. the NUM portion) of the BLOCK1 option or
	 * {@link UintOptionValue#UNDEFINED} if there is no BLOCK1 option present in
	 * this {@link CoapMessage}.
	 *
	 * @return the sequence number of the block1 option or
	 * {@link de.uzl.itm.ncoap.message.options.UintOptionValue#UNDEFINED} if there
	 * is no such option present in this {@link CoapRequest}.
	 */
	public long getBlock1Number()
	{
		if ( !options.containsKey( Option.BLOCK_1 ) ) {
			return UintOptionValue.UNDEFINED;
		} else {
			return (long) options.get( Option.BLOCK_1 ).iterator().next().getDecodedValue() >> 4;
		}
	}

	/**
	 * Returns <code>true</code> if and only if
	 * <ul>
	 * <li>the BLOCK1 option is present and its value indicates that there are no
	 * more blocks to come or</li>
	 * <li>if there is no BLOCK1 option present in this {@link CoapMessage}.</li>
	 * </ul>
	 *
	 * @return <code>true</code> if there are no more blocks expected and
	 * <code>false</code> otherwise.
	 */
	public boolean isLastBlock1()
	{
		if ( !options.containsKey( Option.BLOCK_1 ) ) {
			return true;
		} else {
			long m = (long) options.get( Option.BLOCK_1 ).iterator().next().getDecodedValue();
			return (extractBits( m, 1, 3 ) == 0);
		}
	}

	/**
	 * Returns the encoded block size of the BLOCK1 option (i.e. the SZX portion)
	 * or {@link UintOptionValue#UNDEFINED} if there is no BLOCK1 option contained
	 * in this {@link CoapMessage}.
	 *
	 * With szx as the returned value the actual blocksize is
	 * <code>2^(szx + 4)</code> bytes.
	 *
	 * @return the encoded block size of the BLOCK1 option (i.e. the SZX portion)
	 * or {@link UintOptionValue#UNDEFINED} if there is no BLOCK1 option contained
	 * in this {@link CoapMessage}.
	 */
	public long getBlock1Szx()
	{
		if ( !options.containsKey( Option.BLOCK_1 ) ) {
			return UintOptionValue.UNDEFINED;
		} else {
			long value = (long) options.get( Option.BLOCK_1 ).iterator().next().getDecodedValue();
			return extractBits( value, 3, 0 );
		}
	}

	/**
	 * Returns the decoded size (i.e. number of bytes) given by the SZX portion of
	 * the BLOCK-1 option or {@link UintOptionValue#UNDEFINED} if no BLOCK1 option
	 * is contained in this {@link CoapMessage}.
	 *
	 * @return the decoded size (i.e. number of bytes) given by the SZX portion of
	 * the BLOCK-1 option or {@link UintOptionValue#UNDEFINED} if no BLOCK1 option
	 * is contained in this {@link CoapMessage}.
	 */
	public long getBlock1Size()
	{
		long block1szx = getBlock1Szx();
		if ( block1szx == UintOptionValue.UNDEFINED ) {
			return UintOptionValue.UNDEFINED;
		} else {
			return BlockSize.getBlockSize( block1szx ).getSize();
		}
	}

	public void setSize2( long size2 ) throws IllegalArgumentException
	{
		this.options.remove( Option.SIZE_2 );
		this.addUintOption( Option.SIZE_2, size2 );
	}

	public long getSize2()
	{
		if ( options.containsKey( Option.SIZE_2 ) ) {
			return ((UintOptionValue) options.get( Option.SIZE_2 ).iterator().next()).getDecodedValue();
		} else {
			return UintOptionValue.UNDEFINED;
		}
	}

	public void setSize1( long size1 ) throws IllegalArgumentException
	{
		this.options.remove( Option.SIZE_1 );
		this.addUintOption( Option.SIZE_1, size1 );
	}

	public long getSize1()
	{
		if ( options.containsKey( Option.SIZE_1 ) ) {
			return ((UintOptionValue) options.get( Option.SIZE_1 ).iterator().next()).getDecodedValue();
		} else {
			return UintOptionValue.UNDEFINED;
		}
	}

	/**
	 * Returns the {@link jolie.net.coap.communication.identification.EndpointID}
	 * contained in this message as
	 * {@link de.uzl.itm.ncoap.message.options.Option#ENDPOINT_ID_1} or
	 * <code>null</code> if no such option is present
	 *
	 * @return the {@link jolie.net.coap.communication.identification.EndpointID}
	 * contained in this message as
	 * {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_1} or
	 * <code>null</code> if no such option is present
	 */
	public byte[] getEndpointID1()
	{
		List<OptionValue> values = getOptions( Option.ENDPOINT_ID_1 );
		if ( values.isEmpty() ) {
			return null;
		} else {
			return values.iterator().next().getValue();
		}
	}

	/**
	 * Sets the {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_1} with
	 * an empty byte array. This value is replaces with a valid ID by the
	 * framework during outbound message processing
	 */
	public void setEndpointID1()
	{
		this.setEndpointID1( new byte[ 0 ] );
	}

	/**
	 * Sets the {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_1} with
	 * the given byte array.
	 *
	 * <b>Note:</b> This method is intended for internal use. The given value is
	 * likely to be replaced by the framework during outbound message processing!
	 * Use {@link #setEndpointID1} instead!
	 *
	 * @param value the
	 * {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_1} option value
	 */
	public void setEndpointID1( byte[] value )
	{
		try {
			this.removeOptions( Option.ENDPOINT_ID_1 );
			this.addOpaqueOption( Option.ENDPOINT_ID_1, value );
		} catch( IllegalArgumentException e ) {
			this.removeOptions( Option.ENDPOINT_ID_1 );
		}
	}

	/**
	 * Returns the {@link jolie.net.coap.communication.identification.EndpointID}
	 * contained in this message as
	 * {@link de.uzl.itm.ncoap.message.options.Option#ENDPOINT_ID_2} or
	 * <code>null</code> if no such option is present
	 *
	 * @return the {@link jolie.net.coap.communication.identification.EndpointID}
	 * contained in this message as
	 * {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_2} or
	 * <code>null</code> if no such option is present
	 */
	public byte[] getEndpointID2()
	{
		List<OptionValue> values = getOptions( Option.ENDPOINT_ID_2 );
		if ( values.isEmpty() ) {
			return null;
		} else {
			return values.iterator().next().getValue();
		}
	}

	/**
	 * Sets the {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_2} with
	 * the given byte array.
	 *
	 * <b>Note:</b> This method is intended for internal use. The given value is
	 * likely to be replaced or removed by the framework during outbound message
	 * processing!
	 *
	 * @param value the
	 * {@link jolie.net.coap.message.options.Option#ENDPOINT_ID_1} option value
	 */
	public void setEndpointID2( byte[] value )
	{
		try {
			this.removeOptions( Option.ENDPOINT_ID_2 );
			this.addOpaqueOption( Option.ENDPOINT_ID_2, value );
		} catch( IllegalArgumentException e ) {
			this.removeOptions( Option.ENDPOINT_ID_2 );
		}
	}

	/**
	 * Adds the content to the message. If this {@link CoapMessage} contained any
	 * content prior to the invocation of method, the previous content is removed.
	 *
	 * @param content ByteBuf containing the message content
	 *
	 * @throws java.lang.IllegalArgumentException if the messages code does not
	 * allow content and for the given {@link ByteBuf#readableBytes()} is greater
	 * then zero.
	 */
	public void setContent( ByteBuf content ) throws IllegalArgumentException
	{

		if ( !(MessageCode.allowsContent( this.messageCode ))
			&& content.readableBytes() > 0 ) {
			throw new IllegalArgumentException( String.format( DOES_NOT_ALLOW_CONTENT,
				this.getMessageCodeName() ) );
		}

		this.content = content;
	}

	/**
	 * Adds the content to the message. If this {@link CoapMessage} contained any
	 * content prior to the invocation of method, the previous content is removed.
	 *
	 * @param content ChannelBuffer containing the message content
	 *
	 * @throws java.lang.IllegalArgumentException if the messages code does not
	 * allow content and the given byte array has a length more than zero.
	 */
	public void setContent( byte[] content ) throws IllegalArgumentException
	{
		setContent( Unpooled.wrappedBuffer( content ) );
	}

	/**
	 * Adds the content to the message. If this {@link CoapMessage} contained any
	 * content prior to the invocation of method, the previous content is removed.
	 *
	 * @param content ChannelBuffer containing the message content
	 * @param contentFormat a long value representing the format of the content
	 *
	 * @throws java.lang.IllegalArgumentException if the messages code does not
	 * allow content
	 */
	public void setContent( byte[] content, long contentFormat )
		throws IllegalArgumentException
	{
		setContent( Unpooled.wrappedBuffer( content ), contentFormat );
	}

	/**
	 * Sets the getContent (payload) of this {@link CoapMessage}.
	 *
	 * @param content {@link ByteBuf} containing the message getContent
	 * @param contentFormat a long value representing the format of the getContent
	 * (see {@link ContentFormat} for some predefined numbers (according to the
	 * CoAP specification)
	 *
	 * @throws java.lang.IllegalArgumentException if the messages code does not
	 * allow getContent and for the given {@link ByteBuf#readableBytes()} is
	 * greater then zero.
	 */
	public void setContent( ByteBuf content, long contentFormat )
		throws IllegalArgumentException
	{

		try {
			this.addUintOption( Option.CONTENT_FORMAT, contentFormat );
			if ( !(MessageCode.allowsContent( this.messageCode ))
				&& content.readableBytes() > 0 ) {
				throw new IllegalArgumentException( String.format(
					DOES_NOT_ALLOW_CONTENT, this.getMessageCodeName() ) );
			}
			this.content = content;
		} catch( IllegalArgumentException e ) {
			this.content = Unpooled.EMPTY_BUFFER;
			this.removeOptions( Option.CONTENT_FORMAT );
			Interpreter.getInstance().logSevere( e );
		}
	}

	/**
	 * Returns the messages getContent. If the message does not contain any
	 * getContent, this method returns an empty
	 * {@link ByteBuf} ({@link ByteBuf#EMPTY_BUFFER}).
	 *
	 * @return Returns the messages getContent.
	 */
	public ByteBuf getContent()
	{
		return this.content;
	}

	public byte[] getContentAsByteArray()
	{
		byte[] result = new byte[ this.getContentLength() ];
		this.getContent().readBytes( result, 0, this.getContentLength() );
		return result;
	}

	public int getContentLength()
	{
		return this.content.readableBytes();
	}

	/**
	 *
	 * @param messageCode
	 * @throws IllegalArgumentException
	 */
	public void setMessageCode( int messageCode )
		throws IllegalArgumentException
	{
		if ( !MessageCode.isValidMessageCode( messageCode ) ) {
			throw new IllegalArgumentException( "Invalid message code no. "
				+ messageCode );
		}
		this.messageCode = messageCode;
	}

	/**
	 * Returns a {@link Map} with the option numbers as keys and
	 * {@link OptionValue}s as values. The returned map does not contain options
	 * with default values.
	 *
	 * @return a {@link Map} with the option numbers as keys and
	 * {@link OptionValue}s as values.
	 */
	public Map<Integer, List<OptionValue>> getAllOptions()
	{
		return this.options;
	}

	/**
	 * Returns the {@link OptionValue}s that are explicitly set in this
	 * {@link CoapMessage}. The returned set does not contain options with default
	 * values. If this {@link CoapMessage} does not contain any options of the
	 * given option number, then the returned set is empty.
	 *
	 * @param optionNumber the option number
	 *
	 * @return a {@link Set} containing the {@link OptionValue}s that are
	 * explicitly set in this {@link CoapMessage}.
	 */
	public List<OptionValue> getOptions( int optionNumber )
	{
		return this.options.get( optionNumber );
	}

	private void checkOptionPermission( int optionNumber ) throws IllegalArgumentException
	{

		Option.Occurence permittedOccurence = Option.getPermittedOccurence( optionNumber, this.messageCode );
		if ( permittedOccurence == Option.Occurence.NONE ) {
			throw new IllegalArgumentException( String.format( OPTION_NOT_ALLOWED_WITH_MESSAGE_TYPE,
				optionNumber, Option.asString( optionNumber ), this.getMessageCodeName() ) );
		} else if ( options.containsKey( optionNumber ) && permittedOccurence == Option.Occurence.ONCE ) {
			throw new IllegalArgumentException( String.format( OPTION_ALREADY_SET, optionNumber ) );
		}
	}

	private static long extractBits( final long value, final int bits, final int offset )
	{
		final long shifted = value >>> offset;
		final long masked = (1L << bits) - 1L;
		return shifted & masked;
	}

	/**
	This method retrieves the current message content format as a string.
	@return the String version of the { @link ContentFormat } of this { @link CoapMessage }
	 */
	public String contentFormat()
	{
		return ContentFormat.toString( (long) this.getOptions( Option.CONTENT_FORMAT ).get( 0 ).getDecodedValue() );

	}

	/**
	 * Returns <code>true</code> if an option with the given number is contained
	 * in this {@link CoapMessage} and <code>false</code> otherwise.
	 *
	 * @param optionNumber the option number
	 *
	 * @return <code>true</code> if an option with the given number is contained
	 * in this {@link CoapMessage} and <code>false</code> otherwise.
	 */
	public boolean containsOption( int optionNumber )
	{
		return getOptions( optionNumber ) != null;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode() + content.hashCode();
	}

	@Override
	public boolean equals( Object object )
	{

		if ( !(object instanceof CoapMessage) ) {
			return false;
		}

		CoapMessage other = (CoapMessage) object;

		//Check header fields
		if ( this.getProtocolVersion() != other.getProtocolVersion() ) {
			return false;
		}

		if ( this.messageType() != other.messageType() ) {
			return false;
		}

		if ( this.messageCode() != other.messageCode() ) {
			return false;
		}

		if ( this.id() != other.id() ) {
			return false;
		}

		if ( !this.token().equals( other.token() ) ) {
			return false;
		}

		if ( this.getAllOptions().size() == other.getAllOptions().size() ) {
			return false;
		} else {
			for( Map.Entry<Integer, List<OptionValue>> entry : this.getAllOptions().entrySet() ) {
				Integer key = entry.getKey();
				List<OptionValue> value = entry.getValue();
				if ( !other.getOptions( key ).equals( value ) ) {
					return false;
				}
			}
		}

		//Check setContent
		return this.getContent().equals( other.getContent() );
	}

	@Override
	public String toString()
	{

		StringBuilder result = new StringBuilder();

		//Header + Token
		result.append( "[Header: (V) " ).append( getProtocolVersion() )
			.append( ", (T) " ).append( getMessageTypeName() )
			.append( ", (TKL) " ).append( token.getBytes().length )
			.append( ", (C) " ).append( messageCode() )
			.append( ", (ID) " ).append( id() )
			.append( " | (Token) " ).append( token ).append( " | " );

		//Options
		result.append( "Options:" );
		if ( !this.options.isEmpty() ) {
			for( int optionNumber : getAllOptions().keySet() ) {
				result.append( " (No. " ).append( optionNumber ).append( ") " );
				List<OptionValue> optionValueList = this.getOptions( optionNumber );
				for( OptionValue optionValue : optionValueList ) {
					result.append( optionValue.getDecodedValue().toString() ).append( " " );
				}
			}
		} else {
			result.append( " <no options> " );
		}

		result.append( " | " );

		//Content
		result.append( "Content: " );
		long payloadLength = getContent().readableBytes();
		if ( payloadLength == 0 ) {
			result.append( "<no content>]" );
		} else {
			result.append(
				Unpooled.copiedBuffer( getContent() )
					.toString( CoapMessage.CHARSET ) )
				.append( " ( " ).append( payloadLength ).append( " bytes)]" );
		}

		return result.toString();

	}

	public String operationName()
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}
}
