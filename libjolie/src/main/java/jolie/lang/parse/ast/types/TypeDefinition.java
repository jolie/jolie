/*
 * Copyright (C) 2008 Elvis Ciotti
 * Copyright (C) 2008-2020 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2011 Claudio Guidi
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import jolie.lang.NativeType;
import jolie.lang.parse.DocumentedNode;
import jolie.lang.parse.ast.ImportableSymbol;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import jolie.util.Range;

/**
 * Representation for a type definition.
 *
 * @author Fabrizio Montesi
 */
public abstract class TypeDefinition extends OLSyntaxNode implements DocumentedNode, ImportableSymbol {
	private final String name;
	private final Range cardinality;
	private String document = null;
	private final AccessModifier accessModifier;

	/**
	 * Constructor
	 *
	 * @param context the parsing context for this AST node
	 * @param name the name identifier for this type definition
	 * @param cardinality the cardinality of this type
	 * @param accessModifier the access modifier of symbolinfo
	 */
	public TypeDefinition( ParsingContext context, String name, Range cardinality, AccessModifier accessModifier ) {
		super( context );
		this.name = name;
		this.cardinality = cardinality;
		this.accessModifier = accessModifier;
	}

	public String simpleName() {
		return this.name;
	}

	public Range cardinality() {
		return cardinality;
	}

	@Override
	public void setDocumentation( String document ) {
		this.document = document;
	}

	@Override
	public Optional< String > getDocumentation() {
		return Optional.ofNullable( document );
	}

	public boolean containsPath( VariablePathNode variablePath ) {
		return containsPath( variablePath.path().iterator() );
	}

	protected abstract boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it );

	private static boolean checkTypeEqualnessChoiceChoice( TypeChoiceDefinition left, TypeChoiceDefinition right,
		Set< String > recursiveTypesChecked ) {
		return checkTypeEqualness( left.left(), right.left(), recursiveTypesChecked )
			&&
			checkTypeEqualness( left.right(), right.right(), recursiveTypesChecked );
	}

	/*
	 * 13/10/2011 - Claudio Guidi: added recursiveTypesChecked list for checking recursive types
	 * equalness
	 */
	private static boolean checkTypeEqualnessInline( TypeInlineDefinition left, TypeInlineDefinition right,
		Set< String > recursiveTypesChecked ) {
		if( !left.basicType().checkBasicTypeEqualness( right.basicType() ) ) {
			return false;
		}

		if( left.untypedSubTypes() ) {
			return right.untypedSubTypes();
		} else {
			if( right.untypedSubTypes() ) {
				return false;
			}
			if( left.hasSubTypes() ) {
				if( !right.hasSubTypes() ) {
					return false;
				}

				if( left.subTypes().size() != right.subTypes().size() ) {
					return false;
				}

				for( Entry< String, TypeDefinition > entry : left.subTypes() ) {
					final TypeDefinition rightSubType = right.getSubType( entry.getKey() );
					if( rightSubType == null ) {
						return false;
					}
					if( recursiveTypesChecked.contains( rightSubType.name ) ) {
						return true;
					} else {
						recursiveTypesChecked.add( rightSubType.name );
					}
					if( entry.getValue().isEquivalentTo_recursive( right.getSubType( entry.getKey() ),
						recursiveTypesChecked ) == false ) {
						return false;
					}
				}
			} else {
				return right.hasSubTypes() == false;
			}
		}

		return true;
	}

	private static boolean checkTypeEqualness( TypeDefinition left, TypeDefinition right,
		Set< String > recursiveTypesChecked ) {
		if( left instanceof TypeChoiceDefinition ) {
			final TypeChoiceDefinition choice = (TypeChoiceDefinition) left;
			if( right instanceof TypeInlineDefinition ) {
				return checkTypeEqualness( choice.left(), right, recursiveTypesChecked )
					&&
					checkTypeEqualness( choice.right(), right, recursiveTypesChecked );
			} else if( right instanceof TypeDefinitionLink ) {
				return checkTypeEqualness( left, ((TypeDefinitionLink) right).linkedType(), recursiveTypesChecked );
			} else if( right instanceof TypeChoiceDefinition ) {
				return checkTypeEqualnessChoiceChoice( choice, (TypeChoiceDefinition) right, recursiveTypesChecked );
			}
		} else if( left instanceof TypeDefinitionLink ) {
			return checkTypeEqualness( ((TypeDefinitionLink) left).linkedType(), right, recursiveTypesChecked );
		} else if( left instanceof TypeInlineDefinition ) {
			if( right instanceof TypeInlineDefinition ) {
				return checkTypeEqualnessInline( (TypeInlineDefinition) left, (TypeInlineDefinition) right,
					recursiveTypesChecked );
			} else if( right instanceof TypeDefinitionLink ) {
				return checkTypeEqualness( left, ((TypeDefinitionLink) right).linkedType(), recursiveTypesChecked );
			} else if( right instanceof TypeChoiceDefinition ) {
				final TypeChoiceDefinition choice = (TypeChoiceDefinition) right;
				return checkTypeEqualness( left, choice.left(), recursiveTypesChecked )
					&&
					checkTypeEqualness( right, choice.right(), recursiveTypesChecked );
			}
		}

		return false; // Unaccounted cases, fail fast.
	}

	/**
	 * @author Claudio Guidi 01-Sep-2011 Fabrizio Montesi: removed some type casting
	 */
	public static TypeDefinition extend( TypeDefinition inputType, TypeDefinition extender, String namePrefix ) {
		if( inputType instanceof TypeChoiceDefinition || extender instanceof TypeChoiceDefinition ) {
			throw new UnsupportedOperationException( "extension does not support choice types yet" );
		}

		if( inputType instanceof TypeDefinitionLink ) {
			return extend( ((TypeDefinitionLink) inputType).linkedType(), extender, namePrefix );
		} else if( extender instanceof TypeDefinitionLink ) {
			return extend( inputType, ((TypeDefinitionLink) extender).linkedType(), namePrefix );
		}

		final TypeInlineDefinition left = (TypeInlineDefinition) inputType;
		final TypeInlineDefinition right = (TypeInlineDefinition) extender;

		TypeInlineDefinition newType =
			new TypeInlineDefinition( inputType.context(), namePrefix + "_" + inputType.name(),
				left.basicType(), inputType.cardinality );

		if( left instanceof TypeDefinitionUndefined ) {
			TypeInlineDefinition newTid = new TypeInlineDefinition( inputType.context(),
				namePrefix + "_" + inputType.name(), BasicTypeDefinition.of( NativeType.ANY ), inputType.cardinality );
			if( right.hasSubTypes() ) {
				for( Entry< String, TypeDefinition > subType : right.subTypes() ) {
					newTid.putSubType( subType.getValue() );
				}
			}
			newType = newTid;
		} else {
			if( left.hasSubTypes() ) {
				for( Entry< String, TypeDefinition > subType : left.subTypes() ) {
					newType.putSubType( subType.getValue() );
				}
			}
			if( right.hasSubTypes() ) {
				for( Entry< String, TypeDefinition > subType : right.subTypes() ) {
					newType.putSubType( subType.getValue() );
				}
			}
		}
		return newType;
	}

	/**
	 * Checks if this TypeDeclaration is equivalent to other
	 *
	 * @author Fabrizio Montesi
	 */
	public boolean isEquivalentTo( TypeDefinition other ) {
		Set< String > recursiveTypeChecked = new HashSet<>();
		return cardinality.equals( other.cardinality ) && checkTypeEqualness( this, other, recursiveTypeChecked );
	}

	/**
	 * introduced for checking also recursive type equalness
	 *
	 * @author Claudio Guidi
	 */
	private boolean isEquivalentTo_recursive( TypeDefinition other, Set< String > recursiveTypeChecked ) {
		return cardinality.equals( other.cardinality ) && checkTypeEqualness( this, other, recursiveTypeChecked );
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj )
			return true;
		if( obj == null )
			return false;
		if( getClass() != obj.getClass() )
			return false;
		TypeDefinition other = (TypeDefinition) obj;
		if( !this.name.equals( other.name ) ) {
			return false;
		}
		return this.isEquivalentTo( other );
	}

	protected abstract int hashCode( Set< String > recursiveTypeHashed );

	@Override
	public int hashCode() {
		return hashCode( new HashSet<>() );
	}

	/*
	 * public abstract TypeDefinition getSubType( String id ); public abstract Set< Map.Entry< String,
	 * TypeDefinition > > subTypes(); public abstract boolean hasSubTypes(); public abstract boolean
	 * untypedSubTypes(); public abstract NativeType nativeType(); public abstract boolean hasSubType(
	 * String id );
	 */

	@Override
	public AccessModifier accessModifier() {
		return this.accessModifier;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public OLSyntaxNode node() {
		return this;
	}

}
