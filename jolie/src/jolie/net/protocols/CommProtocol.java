/********************************************************************************
 *   Copyright (C) 2006-2017 by Fabrizio Montesi <famontesi@gmail.com>         *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package jolie.net.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.lang.Constants;
import jolie.net.AbstractCommChannel;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;

/**
 * A CommProtocol implements a protocol for sending and receiving data under the form of CommMessage objects.
 * This class should not be extended directly; see {@link ConcurrentCommProtocol ConcurrentCommProtocol} and {@link SequentialCommProtocol SequentialCommProtocol} instead.
 * @author Fabrizio Montesi
 */
public abstract class CommProtocol
{

	private final static class LazyDummyChannelHolder
	{

		private LazyDummyChannelHolder()
		{
		}

		private static class DummyChannel extends AbstractCommChannel
		{

			@Override
			public void closeImpl()
			{
			}

			@Override
			public void sendImpl( CommMessage message )
			{
			}

			@Override
			public CommMessage recvImpl()
			{
				return CommMessage.UNDEFINED_MESSAGE;
			}

			@Override
			public URI getLocation()
			{
				throw new UnsupportedOperationException( "DummyChannels are not supposed to be queried for their location." );
			}

			@Override
			protected boolean isThreadSafe()
			{
				return false;
			}
		}

		private final static DummyChannel dummyChannel = new DummyChannel();
	}

	private static class Parameters
	{

		private static final String OPERATION_SPECIFIC_CONFIGURATION = "osc";
	}

	private final VariablePath configurationPath;
	private CommChannel channel = null;

	protected VariablePath configurationPath()
	{
		return configurationPath;
	}

	public abstract String name();

	public CommProtocol( VariablePath configurationPath )
	{
		this.configurationPath = configurationPath;
	}

	public void setChannel( CommChannel channel )
	{
		this.channel = channel;
	}

	protected CommChannel channel()
	{
		if ( this.channel == null ) {
			return LazyDummyChannelHolder.dummyChannel;
		}
		return this.channel;
	}

	protected ValueVector getParameterVector( String id )
	{
		return configurationPath.getValue().getChildren( id );
	}

	protected boolean hasParameter( String id )
	{
		if ( configurationPath.getValue().hasChildren( id ) ) {
			Value v = configurationPath.getValue().getFirstChild( id );
			return v.isDefined() || v.hasChildren();
		}
		return false;
	}

	protected boolean hasParameterValue( String id )
	{
		if ( configurationPath.getValue().hasChildren( id ) ) {
			Value v = configurationPath.getValue().getFirstChild( id );
			return v.isDefined();
		}
		return false;
	}

	/**
	 * Shortcut for getParameterVector( id ).first()
	 */
	protected Value getParameterFirstValue( String id )
	{
		return getParameterVector( id ).first();
	}

	/**
	 * Shortcut for checking if a parameter intValue() equals 1
	 * @param id the parameter identifier
	 */
	protected boolean checkBooleanParameter( String id )
	{
		return hasParameter( id ) && getParameterFirstValue( id ).boolValue();
	}

	/**
	 * Shortcut for checking if a parameter intValue() equals 1
	 * @param id the parameter identifier
	 */
	protected boolean checkBooleanParameter( String id, boolean defaultValue )
	{
		if ( hasParameter( id ) ) {
			return getParameterFirstValue( id ).boolValue();
		} else {
			return defaultValue;
		}
	}

	/**
	 * Shortcut for checking if a string parameter has a given value
	 * @param id the parameter identifier
	 * @param value the value for checking the parameter with
	 * @return {@code true} if the parameter has the expected value, {@code false} otherwise
	 */
	protected boolean checkStringParameter( String id, String value )
	{
		if ( hasParameter( id ) ) {
			return getParameterFirstValue( id ).strValue().equals( value );
		} else {
			return false;
		}
	}

	/**
	 * Shortcut for <code>getParameterFirstValue( id ).strValue()</code>
	 * @param id the parameter identifier
	 */
	protected String getStringParameter( String id )
	{
		return getStringParameter( id, "" );
	}

	protected String getStringParameter( String id, String defaultValue )
	{
		return (hasParameter( id ) ? getParameterFirstValue( id ).strValue() : defaultValue);
	}

	protected boolean hasOperationSpecificParameter( String operationName, String parameterName )
	{
		if ( hasParameter( Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
			Value osc = getParameterFirstValue( Parameters.OPERATION_SPECIFIC_CONFIGURATION );
			if ( osc.hasChildren( operationName ) ) {
				return osc.getFirstChild( operationName ).hasChildren( parameterName );
			}
		}
		return false;
	}

	protected String getOperationSpecificStringParameter( String operationName, String parameterName )
	{
		if ( hasParameter( Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
			Value osc = getParameterFirstValue( Parameters.OPERATION_SPECIFIC_CONFIGURATION );
			if ( osc.hasChildren( operationName ) ) {
				Value opConfig = osc.getFirstChild( operationName );
				if ( opConfig.hasChildren( parameterName ) ) {
					return opConfig.getFirstChild( parameterName ).strValue();
				}
			}
		}
		return "";
	}

	/**
	 * Shortcut for getOperationSpecificParameterVector( id ).first()
	 */
	protected Value getOperationSpecificParameterFirstValue( String operationName, String parameterName )
	{
		return getOperationSpecificParameterVector( operationName, parameterName ).first();
	}

	protected ValueVector getOperationSpecificParameterVector( String operationName, String parameterName )
	{
		Value osc = getParameterFirstValue( Parameters.OPERATION_SPECIFIC_CONFIGURATION );
		return osc.getFirstChild( operationName ).getChildren( parameterName );
	}

	/**
	Shortcut for getOperationSpecificParameterVector( operationName, parameterName ).first().boolValue()
	@param operationName
	@param parameterName
	@return 
	 */
	protected boolean getOperationSpecificBooleanParameter( String operationName, String parameterName )
	{
		if ( hasOperationSpecificParameter( operationName, parameterName ) ) {
			return getOperationSpecificParameterVector( operationName, parameterName ).first().boolValue();
		}
		return false;
	}

	/**
	@param start
	@param value
	@return 
	 */
	protected String dynamicAlias( String start, Value value )
	{
		Set<String> aliasKeys = new TreeSet<>();
		String pattern = "%(!)?\\{[^\\}]*\\}";

		// find pattern
		int offset = 0;
		String currStrValue;

		String currKey;

		StringBuilder result = new StringBuilder( start );
		Matcher m = Pattern.compile( pattern ).matcher( start );

		// substitute in alias
		while( m.find() ) {
			currKey = start.substring( m.start() + 3, m.end() - 1 );
			currStrValue = value.getFirstChild( currKey ).strValue();
			aliasKeys.add( currKey );
			result.replace( m.start() + offset, m.end() + offset, currStrValue );
			offset += currStrValue.length() - 3 - currKey.length();
		}

		// remove from the value
		for( String aliasKey : aliasKeys ) {
			value.children().remove( aliasKey );
		}

		return result.toString();
	}

	/**
	Given the <code>aliasStringParameter</code> ( e.g. \"alias\") for an operation, 
	it searches iteratively in the <code>configurationPath</code> of the 
	{@link AsyncCommProtocol} to find the corresponsding <code>operationName</code>.
	@param aliasParameter
	@param alias
	@return 
	 */
	protected String getOperationFromOperationSpecificStringParameter( String aliasParameter, String alias )
	{
		for( Map.Entry<String, ValueVector> first : configurationPath().getValue().children().entrySet() ) {
			String first_level_key = first.getKey();
			ValueVector first_level_valueVector = first.getValue();
			if ( first_level_key.equals( "osc" ) ) {
				for( Iterator<Value> first_iterator = first_level_valueVector.iterator(); first_iterator.hasNext(); ) {
					Value fisrt_value = first_iterator.next();
					for( Map.Entry<String, ValueVector> second : fisrt_value.children().entrySet() ) {
						String operationName = second.getKey();
						ValueVector second_level_valueVector = second.getValue();
						for( Iterator<Value> second_iterator = second_level_valueVector.iterator(); second_iterator.hasNext(); ) {
							Value second_value = second_iterator.next();
							for( Map.Entry<String, ValueVector> third : second_value.children().entrySet() ) {
								String third_level_key = third.getKey();
								ValueVector third_level_valueVector = third.getValue();
								if ( third_level_key.equals( aliasParameter ) ) {
									StringBuilder sb = new StringBuilder( "" );
									for( Iterator<Value> third_iterator = third_level_valueVector.iterator(); third_iterator.hasNext(); ) {
										Value third_value = third_iterator.next();
										sb.append( third_value.strValue() );
									}
									if ( sb.toString().equals( alias ) ) {
										return operationName;
									}
								}
							}
						}
					}
				}
			}
		}
		return alias;
	}

	/**
	Shortcut for Parent Port lookup in the OneWay interface.
	@param operationName
	@return 
	 */
	protected boolean isOneWay( String operationName )
	{
		return channel().parentPort().getInterface().oneWayOperations().containsKey( operationName );
	}

	/**
	Shortcut for Parent Port lookup in the RequestResponse interface.
	@param operationName
	@return 
	 */
	protected boolean isRequestResponse( String operationName )
	{
		return channel().parentPort().getInterface().requestResponseOperations().containsKey( operationName );
	}

	protected Type operationType( String on, boolean isRequest )
	{

		OperationTypeDescription otd = channel().parentPort()
			.getOperationTypeDescription( on, "/" );
		Type type = isOneWay( on ) ? otd.asOneWayTypeDescription().requestType()
			: isRequest
				? otd.asRequestResponseTypeDescription().requestType()
				: otd.asRequestResponseTypeDescription().responseType();
		return type;
	}

	protected Type getSendType( CommMessage message )
		throws IOException
	{
		Type ret = null;

		if ( channel().parentPort() == null ) {
			throw new IOException( "Could not retrieve communication port for:\n"
				+ message.toPrettyString() );
		}

		OperationTypeDescription opDesc = channel().parentPort()
			.getOperationTypeDescription( message.operationName(), Constants.ROOT_RESOURCE_PATH );

		if ( opDesc == null ) {
			return null;
		}

		if ( opDesc.asOneWayTypeDescription() != null ) {
			if ( message.isFault() ) {
				ret = Type.UNDEFINED;
			} else {
				OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
				ret = ow.requestType();
			}
		} else if ( opDesc.asRequestResponseTypeDescription() != null ) {
			RequestResponseTypeDescription rr = opDesc.asRequestResponseTypeDescription();
			if ( message.isFault() ) {
				ret = rr.getFaultType( message.fault().faultName() );
				if ( ret == null ) {
					ret = Type.UNDEFINED;
				}
			} else {
				ret = (channel().parentInputPort() != null) ? rr.responseType() : rr.requestType();
			}
		}

		return ret;
	}

	protected OperationTypeDescription getOperationTypeDescription( String operationName ) throws IOException
	{
		if ( channel().parentPort() == null ) {
			throw new IOException( "Could not retrieve communication port for current protocol" );
		}
		return channel().parentPort().getOperationTypeDescription( operationName, Constants.ROOT_RESOURCE_PATH );
	}

	protected Type getSendType( String operationName )
		throws IOException
	{
		OperationTypeDescription opDesc = getOperationTypeDescription( operationName );

		if ( opDesc == null ) {
			return null;
		}

		if ( opDesc.asOneWayTypeDescription() != null ) {
			OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
			return ow.requestType();
		} else if ( opDesc.asRequestResponseTypeDescription() != null ) {
			RequestResponseTypeDescription rr = opDesc.asRequestResponseTypeDescription();
			return (channel().parentInputPort() != null) ? rr.responseType() : rr.requestType();
		} else {
			return null;
		}
	}
	
	abstract public String getConfigurationHash();
	
	/**
	 * Shortcut for <code>getParameterFirstValue( id ).intValue()</code>
	 * @param id the parameter identifier
	 */
	protected int getIntParameter( String id )
	{
		return (hasParameter( id ) ? getParameterFirstValue( id ).intValue() : 0);
	}

	abstract public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException;

	abstract public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException;

	abstract public boolean isThreadSafe();
}
