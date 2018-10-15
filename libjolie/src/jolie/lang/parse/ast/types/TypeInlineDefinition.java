/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
 *   Copyright (C) 2008 by Elvis Ciotti                                    *
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
public class TypeInlineDefinition extends TypeDefinition
{
	private final NativeType nativeType;
	private Map< String, TypeDefinition > subTypes = null;
	private boolean untypedSubTypes = false;

	public TypeInlineDefinition( ParsingContext context, String id, NativeType nativeType, Range cardinality )
	{
		super( context, id, cardinality );
		this.nativeType = nativeType;
	}

	public NativeType nativeType()
	{
		return nativeType;
	}

	public void setUntypedSubTypes( boolean b )
	{
		untypedSubTypes = b;
	}

	public boolean hasSubType( String id )
	{
		if ( subTypes == null ) {
			return false;
		} else {
			return subTypes.containsKey( id );
		}
	}

	public Set< Map.Entry< String, TypeDefinition > > subTypes()
	{
		if ( subTypes == null ) {
			return null;
		}

		return subTypes.entrySet();
	}
	
	@Override
	protected boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it )
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

	public TypeDefinition getSubType( String id )
	{
		if ( subTypes != null ) {
			return subTypes.get( id );
		}
		return null;
	}

	public boolean hasSubTypes()
	{
		if ( subTypes != null && subTypes.isEmpty() == false ) {
			return true;
		}
		return false;
	}

	public void putSubType( TypeDefinition type )
	{
		if ( subTypes == null ) {
			subTypes = new HashMap<>();
		}
		subTypes.put( type.id(), type );
	}

	public boolean untypedSubTypes()
	{
		return untypedSubTypes;
	}

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
