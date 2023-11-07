/*
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

import java.util.Iterator;
import java.util.Set;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import jolie.util.Range;

/**
 *
 * @author Fabrizio Montesi
 */
public class TypeDefinitionLink extends TypeDefinition {
	private TypeDefinition linkedType;
	private final String linkedTypeName;
	private final String simpleName;

	public TypeDefinitionLink( ParsingContext context, String name, Range cardinality,
		String linkedTypeName ) {
		super( context, name, cardinality, AccessModifier.PUBLIC );
		if( name.matches( "\\d#\\w+" ) ) {
			this.simpleName = name.split( "\\d#" )[ 1 ];
		} else {
			this.simpleName = name;
		}
		this.linkedTypeName = linkedTypeName;
	}

	public TypeDefinitionLink( ParsingContext context, String name, Range cardinality, TypeDefinition linkedType ) {
		super( context, name, cardinality, AccessModifier.PUBLIC );
		if( name.matches( "\\d#\\w" ) ) {
			this.simpleName = name.split( "\\d#" )[ 1 ];
		} else {
			this.simpleName = name;
		}
		this.linkedTypeName = linkedType.name();
		this.linkedType = linkedType;
	}

	public TypeDefinitionLink( ParsingContext context, String name, Range cardinality, AccessModifier accessModifier,
		TypeDefinition linkedType ) {
		super( context, name, cardinality, accessModifier );
		if( name.matches( "\\d#\\w" ) ) {
			this.simpleName = name.split( "\\d#" )[ 1 ];
		} else {
			this.simpleName = name;
		}
		this.linkedTypeName = linkedType.name();
		this.linkedType = linkedType;
	}

	public TypeDefinitionLink( ParsingContext context, String name, Range cardinality, AccessModifier accessModifier,
		String linkedTypeName ) {
		super( context, name, cardinality, accessModifier );
		if( name.matches( "\\d#\\w" ) ) {
			this.simpleName = name.split( "\\d#" )[ 1 ];
		} else {
			this.simpleName = name;
		}
		this.linkedTypeName = linkedTypeName;
	}

	public String linkedTypeName() {
		return linkedTypeName;
	}

	public void setLinkedType( TypeDefinition linkedType ) {
		this.linkedType = linkedType;
	}

	public TypeDefinition linkedType() {
		return linkedType;
	}

	@Override
	protected boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it ) {
		return linkedType.containsPath( it );
	}

	/*
	 * @Override public boolean untypedSubTypes() { return linkedType.untypedSubTypes(); }
	 *
	 * @Override public boolean hasSubTypes() { return linkedType.hasSubTypes(); }
	 *
	 * @Override public TypeDefinition getSubType( String id ) { return linkedType.getSubType( id ); }
	 *
	 * @Override public NativeType nativeType() { return linkedType.nativeType(); }
	 *
	 * @Override public Set< Map.Entry< String, TypeDefinition > > subTypes() { return
	 * linkedType.subTypes(); }
	 *
	 * @Override public boolean hasSubType( String id ) { return linkedType.hasSubType( id ); }
	 */

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

	@Override
	public int hashCode( Set< String > recursiveTypeHashed ) {
		if( recursiveTypeHashed.contains( this.name() ) ) {
			return 0;
		}
		recursiveTypeHashed.add( this.name() );
		final int prime = 31;
		int result = 1;
		result = prime * result + this.name().hashCode();
		result = prime * result + this.cardinality().hashCode();
		result = prime * result + this.linkedTypeName.hashCode();
		result = prime * result + recursiveTypeHashed.size();
		if( linkedType != null ) {
			result = prime * result + linkedType.hashCode( recursiveTypeHashed );
		}
		return result;
	}

	@Override
	public String simpleName() {
		return this.simpleName;
	}
}
