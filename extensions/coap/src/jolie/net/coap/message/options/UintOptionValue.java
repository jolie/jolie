/**********************************************************************************
 *   Copyright (C) 2016, Oliver Kleine, University of Luebeck											*
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
package jolie.net.coap.message.options;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * This class contains all specific functionality for {@link OptionValue}
 * instances with unsigned integer values. If there is any need to access
 * {@link OptionValue} instances directly, e.g. to retrieve its value, one could
 * either cast the option to {@link UintOptionValue} and call
 * {@link #getDecodedValue()} or one could all
 * {@link OptionValue#getDecodedValue()} and cast the return value to
 * {@link Long}.
 *
 * @author Oliver Kleine
 */
public class UintOptionValue extends OptionValue<Long>
{

	/**
	 * Corresponds to a value of <code>-1</code> to indicate that there is no
	 * value for that option set.
	 */
	public static final long UNDEFINED = -1;

	/**
	 * @param optionNumber the option number of the {@link StringOptionValue} to
	 * be created
	 * @param value the value of the {@link StringOptionValue} to be created
	 *
	 * @throws java.lang.IllegalArgumentException if the given option number is
	 * unknown, or if the given value is either the default value or exceeds the
	 * defined length limits for options with the given option number
	 */
	public UintOptionValue( int optionNumber, byte[] value )
		throws IllegalArgumentException
	{
		this( optionNumber, shortenValue( value ), false );
	}

	/**
	 * @param optionNumber the option number of the {@link StringOptionValue} to
	 * be created
	 * @param value the value of the {@link StringOptionValue} to be created
	 * @param allowDefault if set to <code>true</code> no
	 * {@link IllegalArgumentException} is thrown if the given value is the
	 * default value. This may be useful in very special cases, so do not use this
	 * feature if you are not absolutely sure that it is necessary!
	 *
	 * @throws java.lang.IllegalArgumentException if the given option number is
	 * unknown, or if the given value is either the default value or exceeds the
	 * defined length limits for options with the given option number
	 */
	public UintOptionValue( int optionNumber, byte[] value, boolean allowDefault )
		throws IllegalArgumentException
	{
		super( optionNumber, shortenValue( value ), allowDefault );
	}

	@Override
	public Long getDecodedValue()
	{
		return new BigInteger( 1, value ).longValue();
	}

	@Override
	public int hashCode()
	{
		return getDecodedValue().hashCode();
	}

	@Override
	public boolean equals( Object object )
	{
		if ( !(object instanceof UintOptionValue) ) {
			return false;
		}

		UintOptionValue other = (UintOptionValue) object;
		return Arrays.equals( this.getValue(), other.getValue() );
	}

	public static byte[] shortenValue( byte[] value )
	{
		int index = 0;
		while( index < value.length - 1 && value[ index ] == 0 ) {
			index++;
		}

		return Arrays.copyOfRange( value, index, value.length );
	}
}
