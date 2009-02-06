/***************************************************************************
 *   Copyright (C) 2008 by Elvis Ciotti                                    *
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package jolie.lang.parse.ast.types;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.parse.ParsingContext;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.NativeType;
import jolie.util.Range;

abstract public class TypeDefinition extends OLSyntaxNode
{
	final private String id;
	final private Range cardinality;

	public TypeDefinition( ParsingContext context, String id, Range cardinality )
	{
		super( context );
		this.id = id;
		this.cardinality = cardinality;
	}

	public String id()
	{
		return id;
	}

	public Range cardinality()
	{
		return cardinality;
	}

	/**
	 * Checks if this TypeDeclaration is equivalent to otherType.
	 * @author Fabrizio Montesi
	 */
	public boolean equals( TypeDefinition otherType )
	{
		if ( otherType == null ) {
			return false;
		}

		if ( nativeType() != otherType.nativeType() ) {
			return false;
		}

		if ( cardinality.equals( otherType.cardinality ) == false ) {
			return false;
		}

		if ( untypedSubTypes() ) {
			return otherType.untypedSubTypes();
		} else {
			if ( otherType.untypedSubTypes() == false ) {
				return false;
			}
			if ( hasSubTypes() ) {
				if ( subTypes().size() != otherType.subTypes().size() ) {
					return false;
				}

				for( Entry< String, TypeDefinition > entry : subTypes() ) {
					if ( entry.getValue().equals( otherType.getSubType( entry.getKey() ) ) == false ) {
						return false;
					}
				}
			} else {
				return otherType.hasSubTypes() == false;
			}
		}

		return true;
	}

	abstract public TypeDefinition getSubType( String id );
	abstract public Set< Map.Entry< String, TypeDefinition > > subTypes();
	abstract public boolean hasSubTypes();
	abstract public boolean untypedSubTypes();
	abstract public NativeType nativeType();
	abstract public boolean hasSubType( String id );
}
