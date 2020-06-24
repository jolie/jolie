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

	public TypeDefinitionLink( ParsingContext context, String id, Range cardinality,
		String linkedTypeName ) {
		super( context, id, cardinality, AccessModifier.PUBLIC );
		this.linkedTypeName = linkedTypeName;
	}

	public TypeDefinitionLink( ParsingContext context, String id, Range cardinality, TypeDefinition linkedType ) {
		super( context, id, cardinality, AccessModifier.PUBLIC );
		this.linkedTypeName = linkedType.id();
		this.linkedType = linkedType;
	}

	public TypeDefinitionLink( ParsingContext context, String id, Range cardinality, AccessModifier accessModifier,
		TypeDefinition linkedType ) {
		super( context, id, cardinality, accessModifier );
		this.linkedTypeName = linkedType.id();
		this.linkedType = linkedType;
	}

	public TypeDefinitionLink( ParsingContext context, String id, Range cardinality, AccessModifier accessModifier,
		String linkedTypeName ) {
		super( context, id, cardinality, accessModifier );
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
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}

	@Override
	public int hashCode( Set< String > recursiveTypeHashed ) {
		if( recursiveTypeHashed.contains( this.id() ) ) {
			return 0;
		}
		recursiveTypeHashed.add( this.id() );
		final int prime = 31;
		int result = 1;
		result = prime * result + id().hashCode();
		result = prime * result + recursiveTypeHashed.size();
		result = prime * result + linkedTypeName.hashCode();
		if( linkedType != null ) {
			result = prime * result + linkedType.hashCode( recursiveTypeHashed );
		}
		return result;
	}
}
