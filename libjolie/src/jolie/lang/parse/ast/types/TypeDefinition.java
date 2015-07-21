/***************************************************************************
 *   Copyright (C) 2008 by Elvis Ciotti                                    *
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
 *   Copyright (C) 2011 by Claudio Guidi                                *
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.NativeType;
import jolie.lang.parse.DocumentedNode;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.util.Pair;
import jolie.util.Range;

/**
 * Representation for a type definition.
 * @author Fabrizio Montesi
 */
public abstract class TypeDefinition extends OLSyntaxNode implements DocumentedNode
{
	private final String id;
	private final Range cardinality;
	private String document = null;

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
	
	@Override
	public void setDocumentation( String document )
	{
		this.document = document;
	}

	@Override
	public String getDocumentation()
	{
		return this.document;
	}

	public boolean containsPath( VariablePathNode variablePath )
	{
		return containsPath( variablePath.path().iterator() );
	}

	private boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it )
	{
		if ( it.hasNext() == false ) {
			return nativeType() != NativeType.VOID;
		}

		if ( untypedSubTypes() ) {
			return true;
		}

		Pair< OLSyntaxNode, OLSyntaxNode > pair = it.next();
		String nodeName = ((ConstantStringExpression)pair.key()).value();
		if ( hasSubType( nodeName ) ) {
			TypeDefinition subType = getSubType( nodeName );
			return subType.containsPath( it );
		}

		return false;
	}

	/*
	 * 13/10/2011 - Claudio Guidi: added recursiveTypesChecked list for checking recursive types equalness
	 */
	private static boolean checkTypeEqualness( TypeDefinition left, TypeDefinition right, List<String> recursiveTypesChecked )
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
					TypeDefinition rightSubType = right.getSubType( entry.getKey() );
					if ( rightSubType == null ) {
						return false;
					}
					if ( recursiveTypesChecked.contains( rightSubType.id ) ) {
						return true;
					} else {
						recursiveTypesChecked.add( rightSubType.id );
					}
					if ( entry.getValue().isEquivalentTo_recursive( right.getSubType( entry.getKey() ), recursiveTypesChecked ) == false ) {
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
	 * @author Claudio Guidi
	 * 01-Sep-2011 Fabrizio Montesi: removed some type casting
	 */
	public static TypeDefinition extend( TypeDefinition inputType, TypeDefinition extender, String namePrefix )
	{
		TypeInlineDefinition newType = new TypeInlineDefinition( inputType.context(), namePrefix + "_" + inputType.id(), inputType.nativeType(), inputType.cardinality );

		if ( inputType instanceof TypeDefinitionUndefined ) {
			TypeInlineDefinition newTid = new TypeInlineDefinition( inputType.context(), namePrefix + "_" + inputType.id(), NativeType.ANY, inputType.cardinality );
			if ( extender.hasSubTypes() ) {
				for( Entry<String, TypeDefinition> subType : extender.subTypes() ) {
					newTid.putSubType( subType.getValue() );
				}
			}
			newType = newTid;
		} else {
			if ( inputType.hasSubTypes() ) {
				for( Entry<String, TypeDefinition> subType : inputType.subTypes() ) {
					newType.putSubType( subType.getValue() );
				}
			}
			if ( extender.hasSubTypes() ) {
				for( Entry<String, TypeDefinition> subType : extender.subTypes() ) {
					newType.putSubType( subType.getValue() );
				}
			}
		}
		return newType;
	}

	/**
	 * Checks if this TypeDeclaration is equivalent to otherType.
	 * @author Fabrizio Montesi
	 */
	public boolean isEquivalentTo( TypeDefinition other )
	{
		List<String> recursiveTypeChecked = new ArrayList<>();
		return checkTypeEqualness( this, other, recursiveTypeChecked );
	}

	/**
	 * introduced for checking also recursive type equalness
	 * @author Claudio Guidi
	 */
	private boolean isEquivalentTo_recursive( TypeDefinition other, List<String> recursiveTypeChecked )
	{
		return checkTypeEqualness( this, other, recursiveTypeChecked );
	}

	@Override
	public boolean equals( Object other )
	{
		return this == other;
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
