/**********************************************************************************
 *   Copyright (C) 2016, Oliver Kleine, University of Luebeck                     *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>      *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/
package jolie.net.coap.application.linkformat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.options.StringOptionValue;

/**
 * <p>
 * A {@link LinkParam} is a representation of an attribute to describe a
 * resource, resp. its representations. A {@link LinkParam} instance consists of
 * a key and a value, e.g.
 *
 * <ul>
 * <li><code>ct="0 40"</code></li>
 * <li><code>ct=0</code></li>
 * </ul></p>
 *
 * <p>
 * As one can see from the examples the <code>ct</code>-attribute can have
 * different types of values, i.e. multiple values divides by white spaces and
 * enclosed in double-quotes or a single value without double quotes. There are
 * other keys (other than <code>ct</code>) which allow different types of
 * values, too. The static method {@link #getValueType(Key, String)} is to
 * determine the type used for the given value.</p>
 *
 * @author Oliver Kleine
 */
public class LinkParam
{

	//*******************************************************************************************
	// static fields, enums, and methods
	//*******************************************************************************************
	/**
	 * The enumeration {@link Key} contains all link-param-keys that are supported
	 *
	 * @author Oliver Kleine
	 */
	public enum Key
	{
		/**
		 * Corresponds to link-param-key "rel"
		 */
		REL( "rel", ValueType.RELATION_TYPE, ValueType.DQUOTED_RELATION_TYPES ),
		/**
		 * Corresponds to link-param-key "anchor"
		 */
		ANCHOR( "anchor", ValueType.DQUOTED_URI_REFERENCE ),
		/**
		 * Corresponds to link-param-key "rev"
		 */
		REV( "rev", ValueType.RELATION_TYPE ),
		/**
		 * Corresponds to link-param-key "hreflang"
		 */
		HREFLANG( "hreflang", ValueType.LANGUAGE_TAG ),
		/**
		 * Corresponds to link-param-key "media"
		 */
		MEDIA( "media", ValueType.MEDIA_DESC, ValueType.DQUOTED_MEDIA_DESC ),
		/**
		 * Corresponds to link-param-key "title"
		 */
		TITLE( "title", ValueType.DQUOTED_STRING ),
		/**
		 * Corresponds to link-param-key "title*"
		 */
		TITLE_STAR( "title*", ValueType.EXT_VALUE ),
		/**
		 * Corresponds to link-param-key "type"
		 */
		TYPE( "type", ValueType.MEDIA_TYPE, ValueType.DQUOTED_MEDIA_TYPE ),
		/**
		 * Corresponds to link-param-key "rt"
		 */
		RT( "rt", ValueType.RELATION_TYPE ),
		/**
		 * Corresponds to link-param-key "if"
		 */
		IF( "if", ValueType.RELATION_TYPE ),
		/**
		 * Corresponds to link-param-key "sz"
		 */
		SZ( "sz", ValueType.CARDINAL ),
		/**
		 * Corresponds to link-param-key "ct"
		 */
		CT( "ct", ValueType.CARDINAL, ValueType.DQUOTED_CARDINALS ),
		/**
		 * Corresponds to link-param-key "obs"
		 */
		OBS( "obs", ValueType.EMPTY );

//        /**
//         * Used internally for unknown link-param-keys
//         */
//        UNKNOWN(null, ValueType.UNKNOWN);
		private final String keyName;
		private final Set<ValueType> valueTypes;

		Key( String keyName, ValueType... valueType )
		{
			this.keyName = keyName;
			this.valueTypes = new HashSet<>( valueType.length );
			this.valueTypes.addAll( Arrays.asList( valueType ) );
		}

		/**
		 * Returns the name of this link-param-key (i.e. "ct")
		 *
		 * @return the name of this link-param-key (i.e. "ct")
		 */
		public String getKeyName()
		{
			return this.keyName;
		}

		/**
		 * Returns the {@link ValueType}s that are allowed for values of this key
		 *
		 * @return the {@link ValueType}s that are allowed for values of this key
		 */
		public Set<ValueType> getValueTypes()
		{
			return this.valueTypes;
		}
	}

	/**
	 * The enumeration {@link ValueType} contains all value types that are
	 * supported
	 *
	 * @author Oliver Kleine
	 */
	public enum ValueType
	{

		/**
		 * Corresponds to the empty type, i.e. no value
		 */
		EMPTY( false, false ),
		/**
		 * Corresponds to a single value of type "relation-types"
		 */
		RELATION_TYPE( false, false ),
		/**
		 * Corresponds to one or more values of type "relation-types" enclosed in
		 * double-quotes (<code>DQUOTE</code>)
		 */
		DQUOTED_RELATION_TYPES( true, true ),
		/**
		 * Corresponds to a single value of type "URI reference"
		 */
		DQUOTED_URI_REFERENCE( true, false ),
		/**
		 * Corresponds to a single value of type "Language-Tag"
		 */
		LANGUAGE_TAG( false, false ),
		/**
		 * Corresponds to a single value of type "Media Desc"
		 */
		MEDIA_DESC( false, false ),
		/**
		 * Corresponds to a single value of type "Media Desc" enclosed in
		 * double-quotes (<code>DQUOTE</code>)
		 */
		DQUOTED_MEDIA_DESC( true, false ),
		/**
		 * Corresponds to a single value of type "quoted-string", i.e. a string
		 * value enclosed in double-quotes (<code>DQUOTE</code>)
		 */
		DQUOTED_STRING( true, false ),
		/**
		 * Corresponds to a single value of type "ext-value"
		 */
		EXT_VALUE( false, false ),
		/**
		 * Corresponds to a single value of type "media-type"
		 */
		MEDIA_TYPE( false, false ),
		/**
		 * Corresponds to a single value of type "media-type" enclosed in
		 * double-quotes (<code>DQUOTE</code>)
		 */
		DQUOTED_MEDIA_TYPE( true, false ),
		/**
		 * Corresponds to a single value of type "cardinal", i.e. digits
		 */
		CARDINAL( false, false ),
		/**
		 * Values of this type consist of multiple cardinal values, divided by white
		 * spaces and enclosed in double-quotes (<code>DQUOTE</code>)
		 */
		DQUOTED_CARDINALS( true, true );

//        /**
//         * Internally used to represent all other types
//         */
//        UNKNOWN(false, false);
		private boolean doubleQuoted;
		private boolean multipleValues;

		ValueType( boolean doubleQuoted, boolean multipleValues )
		{
			this.doubleQuoted = doubleQuoted;
			this.multipleValues = multipleValues;
		}

		/**
		 * Returns <code>true</code> if this {@link ValueType} allows multiple
		 * values divided by white spaces and <code>false</code> otherwise
		 *
		 * @return <code>true</code> if this {@link ValueType} allows multiple
		 * values divided by white spaces and <code>false</code> otherwise
		 */
		public boolean isMultipleValues()
		{
			return this.multipleValues;
		}

		/**
		 * Returns <code>true</code> if values of this {@link ValueType} are
		 * enclosed in double-quotes (<code>DQUOTE</code>) and <code>false</code>
		 * otherwise
		 *
		 * @return <code>true</code> if values of this {@link ValueType} are
		 * enclosed in double-quotes (<code>DQUOTE</code>) and <code>false</code>
		 * otherwise
		 */
		public boolean isDoubleQuoted()
		{
			return this.doubleQuoted;
		}
	}

	/**
	 * Returns the {@link Key} corresponding to the given name or
	 * <code>null</code> if no such {@link Key} exists
	 *
	 * @param keyName the name of the {@link Key} to lookup
	 * @return the {@link Key} corresponding to the given name or
	 * <code>null</code> if no such {@link Key} exists
	 */
	public static Key getKey( String keyName )
	{
		for( Key key : Key.values() ) {
			if ( key.getKeyName().equals( keyName ) ) {
				return key;
			}
		}
		return null;
	}

	/**
	 * Returns the {@link ValueType} that corresponds to the given key-value-pair
	 * or <code>null</code> if no such {@link ValueType} exists.
	 *
	 * @param key the key
	 * @param value the value
	 *
	 * @return the {@link ValueType} that corresponds to the given key-value-pair
	 * or <code>null</code> if no such {@link ValueType} exists.
	 */
	public static ValueType getValueType( Key key, String value )
	{
		// determine possible value types
		Set<ValueType> valueTypes = key.getValueTypes();

		// check if link param value is quoted and if there is quoted type
		if ( valueTypes.size() == 1 ) {
			return valueTypes.iterator().next();
		} else if ( value.startsWith( "\"" ) && value.endsWith( "\"" ) ) {
			for( ValueType valueType : valueTypes ) {
				if ( valueType.isDoubleQuoted() ) {
					return valueType;
				}
			}
		} else {
			for( ValueType valueType : valueTypes ) {
				if ( !valueType.isDoubleQuoted() ) {
					return valueType;
				}
			}
		}

		return null;
	}

	/**
	 * Decodes the given (serialized) link param (e.g. <code>ct=40</code>)
	 *
	 * @param linkParam the serialized link param
	 * @return an instance of {@link LinkParam} according to the given parameter
	 */
	public static LinkParam decode( String linkParam )
	{
		// remove percent encoding
		byte[] tmp = StringOptionValue.convertToByteArrayWithoutPercentEncoding( linkParam );
		linkParam = new String( tmp, CoapMessage.CHARSET );

		// determine the key of this link param
		String keyName = !linkParam.contains( "=" ) ? linkParam : linkParam.substring( 0, linkParam.indexOf( "=" ) );
		LinkParam.Key key = LinkParam.getKey( keyName );

		if ( key == null ) {
			return null;
		} else if ( keyName.equals( linkParam ) ) {
			// empty attribute
			if ( !key.getValueTypes().contains( ValueType.EMPTY ) ) {
				return null;
			} else {
				return new LinkParam( key, ValueType.EMPTY, null );
			}
		} else {
			// link param has non-empty value
			String value = linkParam.substring( linkParam.indexOf( "=" ) + 1, linkParam.length() );
			LinkParam.ValueType valueType = LinkParam.getValueType( key, value );

			if ( valueType == null ) {
				return null;
			} else {
				return new LinkParam( key, valueType, value );
			}
		}
	}

	/**
	 * <p>
	 * Creates a new instance of {@link LinkParam}</p>
	 *
	 * <p>
	 * <b>Note:</b>For some kinds of link params the enclosing double quotes are
	 * part of the value (e.g. value "0 41" for {@link Key#CT} or "Some title" for
	 * {@link Key#TITLE}). Thus, the latter is created using
	 * <code>createLinkParam(Key.TITLE, "\"Some title\"")</code>
	 * </p>
	 *
	 * @param key the {@link Key} of the link param to be created
	 * @param value the value of the link param to be created (see note above)
	 *
	 * @return a new instance of {@link LinkParam} according to the given
	 * parameters (key and value)
	 */
	public static LinkParam createLinkParam( Key key, String value )
	{
		ValueType valueType = getValueType( key, value );
		if ( valueType == null ) {
			return null;
		} else {
			return new LinkParam( key, valueType, value );
		}
	}

	//******************************************************************************************
	// instance related fields and methods
	//******************************************************************************************
	private Key key;
	private ValueType valueType;
	private String value;

	private LinkParam( Key key, ValueType valueType, String value )
	{
		this.key = key;
		this.valueType = valueType;
		// remove double quotes if existing
		this.value = valueType.isDoubleQuoted() ? value.substring( 1, value.length() - 1 ) : value;
	}

	/**
	 * Returns the {@link Key} of this {@link LinkParam}
	 *
	 * @return the {@link Key} of this {@link LinkParam}
	 */
	public Key getKey()
	{
		return key;
	}

	/**
	 * Shortcut for {@link #getKey()#getKeyName()}
	 *
	 * @return the name of the {@link Key} of this {@link LinkParam} (e.g. "ct" or
	 * "rt")
	 */
	public String getKeyName()
	{
		return this.key.getKeyName();
	}

	/**
	 * Returns the {@link ValueType} of the value returned by {@link #getValue()}
	 *
	 * @return the {@link ValueType} of the value returned by {@link #getValue()}
	 */
	public ValueType getValueType()
	{
		return this.valueType;
	}

	/**
	 * Returns the value of this {@link LinkParam}
	 *
	 * @return the value of this {@link LinkParam}
	 */
	public String getValue()
	{
		if ( this.valueType.isDoubleQuoted() ) {
			return "\"" + this.value + "\"";
		} else {
			return this.value;
		}
	}

	/**
	 * <p>
	 * Returns <code>true</code> if the given value is contained in the value
	 * returned by {@link #getValue()} and <code>false</code> otherwise. The exact
	 * behaviour depends on whether there are multiple values allowed in a single
	 * param (see: {@link ValueType#isMultipleValues()}).</p>
	 *
	 * <p>
	 * Example: If the {@link LinkParam} corresponds to <code>ct="0 41"</code>
	 * then both, <code>contains("0")</code> and <code>contains("41")</code>
	 * return <code>true</code> but <code>contains("0 41")</code> returns
	 * <code>false</code>.</p>
	 *
	 * @param value the value to check
	 *
	 * @return <code>true</code> if the given value is contained in the value
	 * returned by {@link #getValue()} and <code>false</code> otherwise.
	 */
	public boolean contains( String value )
	{
		if ( this.valueType.isMultipleValues() ) {
			return Arrays.asList( this.value.split( " " ) ).contains( value );
		} else {
			return this.value.equals( value );
		}
	}

	/**
	 * Returns a string representation of this {@link LinkParam}
	 *
	 * @return a string representation of this {@link LinkParam}
	 */
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append( this.key.getKeyName() );
		if ( this.valueType != ValueType.EMPTY ) {
			builder.append( "=" );
			if ( this.valueType.doubleQuoted ) {
				builder.append( "\"" );
			}
			builder.append( this.value );
			if ( this.valueType.doubleQuoted ) {
				builder.append( "\"" );
			}
		}
		return builder.toString();
	}
}
