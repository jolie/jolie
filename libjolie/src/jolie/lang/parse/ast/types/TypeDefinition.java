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

/**
 * Representation for a type definition.
 * @author Fabrizio Montesi
 */
abstract public class TypeDefinition extends OLSyntaxNode
{
	private final String id;
	private final Range cardinality;

	/**
	 * Constructor
	 * @param context the parsing context for this AST node
	 * @param id the name identifier for this type definition
	 * @param cardinality the cardinality of this type
	 */
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

	private static boolean checkTypeEqualness( TypeDefinition left, TypeDefinition right )
	{
		if ( left.nativeType() != right.nativeType() ) {
			return false;
		}

		if ( left.cardinality.equals( right.cardinality ) == false ) {
			return false;
		}

		if ( left.untypedSubTypes() ) {
			return right.untypedSubTypes();
		} else {
			if ( right.untypedSubTypes() ) {
				return false;
			}
			if ( left.hasSubTypes() ) {
				if ( left.subTypes().size() != right.subTypes().size() ) {
					return false;
				}

				for( Entry< String, TypeDefinition > entry : left.subTypes() ) {
					if ( entry.getValue().equals( right.getSubType( entry.getKey() ) ) == false ) {
						return false;
					}
				}
			} else {
				return right.hasSubTypes() == false;
			}
		}

		return true;
	}

	/**
	 * Checks if this TypeDeclaration is equivalent to otherType.
	 * @author Fabrizio Montesi
	 */
	@Override
	public boolean equals( Object other )
	{
		if ( this == other ) {
			return true;
		}

		if ( other instanceof TypeDefinition ) {
			return checkTypeEqualness( this, (TypeDefinition)other );
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 31 * hash + this.id.hashCode();
		hash = 31 * hash + this.cardinality.hashCode();
		return hash;
	}

	public abstract TypeDefinition getSubType( String id );
	public abstract Set< Map.Entry< String, TypeDefinition > > subTypes();
	public abstract boolean hasSubTypes();
	public abstract boolean untypedSubTypes();
	public abstract NativeType nativeType();
	public abstract boolean hasSubType( String id );
}
