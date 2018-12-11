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

package jolie.net.coap.application.linkformat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * A {@link LinkValue} is a representation of a single entry in a
 * {@link LinkValueList}. It consists of one URI reference, i.e. the resource to
 * be described, and zero or more instances of {@link LinkParam}, e.g.
 * <code>, e.g. <code>&lt;</example&gt;;ct="0 40"</code>.</p>
 *
 * <p>
 * The notations are taken from RFC 6690</p>
 *
 * @author Oliver Kleine
 */
public class LinkValue
{

	//*******************************************************************************************
	// static fields and methods
	//*******************************************************************************************
	static LinkValue decode( String linkValue )
	{
		LinkValue result = new LinkValue( getUriReference( linkValue ) );
		for( String linkParam : LinkValue.getLinkParams( linkValue ) ) {
			result.addLinkParam( LinkParam.decode( linkParam ) );
		}
		return result;
	}

	private static String getUriReference( String linkValue )
	{
		String uriReference = linkValue.substring( linkValue.indexOf( "<" ) + 1, linkValue.indexOf( ">" ) );
		return uriReference;
	}

	private static List<String> getLinkParams( String linkValue )
	{
		List<String> result = new ArrayList<>();
		String[] linkParams = linkValue.split( ";" );
		result.addAll( Arrays.asList( linkParams ).subList( 1, linkParams.length ) );
		return result;
	}

	//******************************************************************************************
	// instance related fields and methods
	//******************************************************************************************
	private String uriReference;
	private Collection<LinkParam> linkParams;

	/**
	 * Creates a new instance of {@link LinkValue}
	 *
	 * @param uriReference the URI reference, i.e. the resource to be described
	 * @param linkParams the {@link LinkParam}s to describe the resource
	 */
	public LinkValue( String uriReference, Collection<LinkParam> linkParams )
	{
		this.uriReference = uriReference;
		this.linkParams = linkParams;
	}

	private LinkValue( String uriReference )
	{
		this( uriReference, new ArrayList<LinkParam>() );
	}

	private void addLinkParam( LinkParam linkParams )
	{
		this.linkParams.add( linkParams );
	}

	/**
	 * Returns the URI reference of this {@link LinkValue}
	 *
	 * @return the URI reference of this {@link LinkValue}
	 */
	public String getUriReference()
	{
		return this.uriReference;
	}

	/**
	 * Returns the {@link LinkParam}s describing the resource identified by the
	 * URI reference
	 *
	 * @return the {@link LinkParam}s describing this resource identified by the
	 * URI reference
	 */
	public Collection<LinkParam> getLinkParams()
	{
		return this.linkParams;
	}

	/**
	 * Returns <code>true</code> if this {@link LinkValue} contains a
	 * {@link LinkParam} that matches the given criterion, i.e. the given
	 * key-value-pair and <code>false</code> otherwise.
	 *
	 * @param key the key of the criterion
	 * @param value the value of the criterion
	 *
	 * @return <code>true</code> if this {@link LinkValue} contains a
	 * {@link LinkParam} that matches the given criterion, i.e. the given
	 * key-value-pair and <code>false</code> otherwise.
	 */
	public boolean containsLinkParam( LinkParam.Key key, String value )
	{
		for( LinkParam linkParam : this.linkParams ) {
			if ( key.equals( linkParam.getKey() ) ) {
				return value == null || linkParam.contains( value );
			}
		}
		return false;
	}

	/**
	 * Returns a string representation of this {@link LinkValue}.
	 *
	 * @return a string representation of this {@link LinkValue}.
	 */
	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append( "<" ).append( uriReference ).append( ">" );
		for( LinkParam linkParam : this.getLinkParams() ) {
			builder.append( ";" ).append( linkParam.toString() );
		}
		return builder.toString();
	}
}
