/*
 * Copyright (C) 2008 Elvis Ciotti
 * Copyright (C) 2009-2020 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse.ast.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jolie.lang.NativeType;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import jolie.util.Range;

/**
 * @author Fabrizio Montesi
 */
public class TypeInlineDefinition extends TypeDefinition {
	private final BasicTypeDefinition basicTypeDefinition;
	private Map< String, TypeDefinition > subTypes = null;
	private boolean untypedSubTypes = false;

	public TypeInlineDefinition( ParsingContext context, String name, BasicTypeDefinition basicTypeDefinition,
		Range cardinality,
		AccessModifier accessModifier ) {
		super( context, name, cardinality, accessModifier );
		this.basicTypeDefinition = basicTypeDefinition;
	}

	public TypeInlineDefinition( ParsingContext context, String name, BasicTypeDefinition basicTypeDefinition,
		Range cardinality ) {
		this( context, name, basicTypeDefinition, cardinality, AccessModifier.PUBLIC );
	}

	public BasicTypeDefinition basicType() {
		return basicTypeDefinition;
	}


	public void setUntypedSubTypes( boolean b ) {
		untypedSubTypes = b;
	}

	public boolean hasSubType( String name ) {
		if( subTypes == null ) {
			return false;
		} else {
			return subTypes.containsKey( name );
		}
	}

	public Set< Map.Entry< String, TypeDefinition > > subTypes() {
		if( subTypes == null ) {
			return null;
		}

		return subTypes.entrySet();
	}

	@Override
	protected boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it ) {
		if( it.hasNext() == false ) {
			return basicType().nativeType() != NativeType.VOID;
		}

		if( untypedSubTypes() ) {
			return true;
		}

		Pair< OLSyntaxNode, OLSyntaxNode > pair = it.next();
		String nodeName = ((ConstantStringExpression) pair.key()).value();
		if( hasSubType( nodeName ) ) {
			TypeDefinition subType = getSubType( nodeName );
			return subType.containsPath( it );
		}

		return false;
	}

	public TypeDefinition getSubType( String name ) {
		if( subTypes != null ) {
			return subTypes.get( name );
		}
		return null;
	}

	public boolean hasSubTypes() {
		return subTypes != null && subTypes.isEmpty() == false;
	}

	public void putSubType( TypeDefinition type ) {
		if( subTypes == null ) {
			subTypes = new HashMap<>();
		}
		subTypes.put( type.name(), type );
	}

	public boolean untypedSubTypes() {
		return untypedSubTypes;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

	@Override
	protected int hashCode( Set< String > recursiveTypeHashed ) {
		if( recursiveTypeHashed.contains( this.name() ) ) {
			return 0;
		}
		recursiveTypeHashed.add( this.name() );
		final int prime = 31;
		int result = 1;
		result = prime * result + this.name().hashCode();
		result = prime * result + this.cardinality().hashCode();
		result = prime * result + this.basicType().hashCode();
		result = prime * result + recursiveTypeHashed.size();
		if( this.hasSubTypes() ) {
			for( TypeDefinition typeDef : subTypes.values() ) {
				result = prime * result + typeDef.hashCode( recursiveTypeHashed );
			}
		}
		result = prime * result + (untypedSubTypes ? 1231 : 1237);
		return result;
	}
}
