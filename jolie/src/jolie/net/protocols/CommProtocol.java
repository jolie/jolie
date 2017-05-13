/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.net.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import jolie.StatefulContext;
import jolie.net.AbstractCommChannel;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.StatefulMessage;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

/**
 * A CommProtocol implements a protocol for sending and receiving data under the form of CommMessage objects.
 * This class should not be extended directly; see {@link ConcurrentCommProtocol ConcurrentCommProtocol} and {@link SequentialCommProtocol SequentialCommProtocol} instead.
 * @author Fabrizio Montesi
 */
public abstract class CommProtocol
{
	private final static class LazyDummyChannelHolder {
		private LazyDummyChannelHolder() {}
		private static class DummyChannel extends AbstractCommChannel {
			public void closeImpl() {}
			public void sendImpl( StatefulMessage message, Runnable completionHandler, Consumer< Throwable > failureHandler ) {}
			public CommMessage recvImpl() { return CommMessage.UNDEFINED_MESSAGE; }
		}

		private final static DummyChannel dummyChannel = new DummyChannel();
	}

	private static class Parameters {
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
	
	protected ValueVector getParameterVector( StatefulContext ctx, String id )
	{
		return configurationPath.getValue( ctx ).getChildren( id );
	}
	
	protected boolean hasParameter( StatefulContext ctx, String id )
	{
		if ( configurationPath.getValue( ctx ).hasChildren( id ) ) {
			Value v = configurationPath.getValue( ctx ).getFirstChild( id );
			return v.isDefined() || v.hasChildren();
		}
		return false;
	}
	
	protected boolean hasParameterValue( StatefulContext ctx, String id )
	{
		if ( configurationPath.getValue( ctx ).hasChildren( id ) ) {
			Value v = configurationPath.getValue( ctx ).getFirstChild( id );
			return v.isDefined();
		}
		return false;
	}
	
	/**
	 * Shortcut for getParameterVector( id ).first()
	 */
	protected Value getParameterFirstValue( StatefulContext ctx, String id )
	{
		return getParameterVector( ctx, id ).first();
	}
	
	/**
	 * Shortcut for checking if a parameter intValue() equals 1
	 * @param id the parameter identifier
	 */
	protected boolean checkBooleanParameter( StatefulContext ctx, String id )
	{
		return hasParameter( ctx, id ) && getParameterFirstValue( ctx, id ).boolValue();
	}

	/**
	 * Shortcut for checking if a parameter intValue() equals 1
	 * @param id the parameter identifier
	 */
	protected boolean checkBooleanParameter( StatefulContext ctx, String id, boolean defaultValue )
	{
		if ( hasParameter( ctx, id ) ) {
			return getParameterFirstValue( ctx, id ).boolValue();
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
	protected boolean checkStringParameter( StatefulContext ctx, String id, String value )
	{
		if ( hasParameter( ctx, id ) ) {
			return getParameterFirstValue( ctx, id ).strValue().equals( value );
		} else {
			return false;
		}
	}
	
	/**
	 * Shortcut for <code>getParameterFirstValue( id ).strValue()</code>
	 * @param id the parameter identifier
	 */
	protected String getStringParameter( StatefulContext ctx, String id )
	{
		return getStringParameter( ctx, id, "" );
	}
	
	protected String getStringParameter( StatefulContext ctx, String id, String defaultValue )
	{
		return ( hasParameter( ctx, id ) ? getParameterFirstValue( ctx, id ).strValue() : defaultValue );
	}

	protected boolean hasOperationSpecificParameter( StatefulContext ctx, String operationName, String parameterName )
	{
		if ( hasParameter( ctx, Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
			Value osc = getParameterFirstValue( ctx, Parameters.OPERATION_SPECIFIC_CONFIGURATION );
			if ( osc.hasChildren( operationName ) ) {
				return osc.getFirstChild( operationName ).hasChildren( parameterName );
			}
		}
		return false;
	}

	protected String getOperationSpecificStringParameter(  StatefulContext ctx, String operationName, String parameterName )
	{
		if ( hasParameter( ctx, Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
			Value osc = getParameterFirstValue( ctx, Parameters.OPERATION_SPECIFIC_CONFIGURATION );
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
	protected Value getOperationSpecificParameterFirstValue( StatefulContext ctx, String operationName, String parameterName )
	{
		return getOperationSpecificParameterVector( ctx, operationName, parameterName ).first();
	}

	protected ValueVector getOperationSpecificParameterVector( StatefulContext ctx, String operationName, String parameterName )
	{
		Value osc = getParameterFirstValue( ctx, Parameters.OPERATION_SPECIFIC_CONFIGURATION );
		return osc.getFirstChild( operationName ).getChildren( parameterName );
	}

	/**
	 * Shortcut for <code>getParameterFirstValue( id ).intValue()</code>
	 * @param id the parameter identifier
	 */
	protected int getIntParameter( StatefulContext ctx, String id )
	{
		return ( hasParameter( ctx, id ) ? getParameterFirstValue( ctx, id ).intValue() : 0 );
	}
	
	abstract public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException;

	abstract public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException;

	abstract public boolean isThreadSafe();
}
