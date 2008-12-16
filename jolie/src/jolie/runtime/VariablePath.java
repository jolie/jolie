/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.runtime;

import java.util.List;
import java.util.Vector;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.process.TransformationReason;
import jolie.util.Pair;

public class VariablePath implements Expression, Cloneable
{	
	final private Pair< Expression, Expression >[] path; // Right Expression may be null
	final private boolean global;
	final private Value root;
	
	public boolean isGlobal()
	{
		return global;
	}
	
	@Override
	public VariablePath clone()
	{
		List< Pair< Expression, Expression > > list =
			new Vector< Pair< Expression, Expression > >();
		for( Pair< Expression, Expression > p : path )
			list.add( new Pair< Expression, Expression >( p.key(), p.value() ) );
		return new VariablePath( list, global );
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		List< Pair< Expression, Expression > > list =
			new Vector< Pair< Expression, Expression > >();
		for( Pair< Expression, Expression > p : path )
			list.add( new Pair< Expression, Expression >( p.key().cloneExpression( reason ), ( p.value() == null ) ? null : p.value().cloneExpression( reason ) ) );
		return new VariablePath( list, global );
	}

	public VariablePath containedSubPath( VariablePath otherVarPath )
	{
		// If one is global and the other is not, it's not a subpath.
		if ( global != otherVarPath.global )
			return null;

		// If the other path is shorter than this, it's not a subpath.
		if ( otherVarPath.path.length < path.length )
			return null;

		int i, myIndex, otherIndex;
		Pair< Expression, Expression > pair, otherPair;
		Expression expr, otherExpr;
		for( i = 0; i < path.length; i++ ) {
			pair = path[i];
			otherPair = otherVarPath.path[i];
			
			// *.element_name is not a subpath of *.other_name
			if ( !pair.key().evaluate().strValue().equals( otherPair.key().evaluate().strValue() ) )
				return null;
			
			// If element name is equal, check for the same index
			expr = pair.value();
			otherExpr = otherPair.value();
			
			myIndex = ( expr == null ) ? 0 : expr.evaluate().intValue();
			otherIndex = ( otherExpr == null ) ? 0 : otherExpr.evaluate().intValue();
			if ( myIndex != otherIndex )
				return null;
		}
		
		// Now i represents the beginning of the subpath, we can just copy it from there
		List< Pair< Expression, Expression > > subPath =
			new Vector< Pair< Expression, Expression > >();
		for( ; i < otherVarPath.path.length; i++ )
			subPath.add( otherVarPath.path[i] );
		
		return new VariablePath( subPath, global );
	}

	public VariablePath(
			List< Pair< Expression, Expression > > path,
			boolean global
			)
	{
		this.path = new Pair[ path.size() ];
		for( int i = 0; i < path.size(); i++ ) {
			this.path[i] = path.get( i );
		}
		this.global = global;
		this.root = null;
	}
	
	public VariablePath( Value root )
	{
		this.path = new Pair[0];
		this.root = root;
		this.global = false;
	}
	
	private Value getRootValue()
	{
		if ( root != null ) {
			return root;
		} else if ( global ) {
			return Interpreter.getInstance().globalValue();
		}

		return ExecutionThread.currentThread().state().root();
	}
	
	public void undef()
	{
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;
		String keyStr;

		for( int i = 0; i < path.length; i++ ) {
			pair = path[i];
			keyStr = pair.key().evaluate().strValue();
			currVector = currValue.children().get( keyStr );
			if ( currVector == null || currVector.size() < 1 )
				return;
			if ( pair.value() == null ) {
				if ( (i+1) < path.length ) {
					currValue = currVector.get( 0 );
				} else { // We're finished
					currValue.children().remove( keyStr );
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( (i+1) < path.length ) {
					if ( currVector.size() <= index )
						return;
					currValue = currVector.get( index );
				} else {
					if ( currVector.size() > index )
						currVector.remove( index );
				}
			}
		}
	}
	
	public Value getValue()
	{
		return getValue( getRootValue() );
	}

	public Value getValue( Value rootValue )
	{
		Value currValue = rootValue;
		String keyStr;
		for( Pair< Expression, Expression > pair : path ) {
			keyStr = pair.key().evaluate().strValue();
			if ( pair.value() == null )
				currValue =
					currValue.getFirstChild( keyStr );
			else
				currValue =
					currValue.getChildren( keyStr ).get( pair.value().evaluate().intValue() );
		}

		return currValue;
	}
	
	public Value getValueOrNull()
	{
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		for( int i = 0; i < path.length; i++ ) {
			pair = path[i];
			currVector = currValue.children().get( pair.key().evaluate().strValue() );
			if ( currVector == null ) {
				return null;
			}
			if ( pair.value() == null ) {
				if ( (i+1) < path.length ) {
					if ( currVector.isEmpty() ) {
						return null;
					}
					currValue = currVector.get( 0 );
				} else { // We're finished
					if ( currVector.isEmpty() ) {
						return null;
					} else {
						return currVector.get( 0 );
					}
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( currVector.size() <= index ) {
					return null;
				}
				currValue = currVector.get( index );
				if ( (i+1) >= path.length ) {
					return currValue;
				}
			}
		}

		return null;
	}
	
	public ValueVector getValueVector( Value rootValue )
	{
		Pair< Expression, Expression > pair;
		ValueVector currVector = null;
		Value currValue = rootValue;

		for( int i = 0; i < path.length; i++ ) {
			pair = path[i];
			currVector = currValue.getChildren( pair.key().evaluate().strValue() );
			if ( (i+1) < path.length ) {
				if ( pair.value() != null ) {
					currValue = currVector.get( pair.value().evaluate().intValue() );
				} else {
					currValue = currVector.get( 0 );
				}
			}
		}
		return currVector;
	}
	
	public ValueVector getValueVector()
	{
		return getValueVector( getRootValue() );
	}
	
	public void makePointer( VariablePath rightPath )
	{
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;
		String keyStr;

		for( int i = 0; i < path.length; i++ ) {
			pair = path[i];
			keyStr = pair.key().evaluate().strValue();
			currVector = currValue.getChildren( keyStr );
			if ( pair.value() == null ) {
				if ( (i+1) < path.length ) {
					currValue = currVector.get( 0 );
				} else { // We're finished
					currValue.children().put( keyStr, ValueVector.createLink( rightPath ) );
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( (i+1) < path.length ) {
					currValue = currVector.get( index );
				} else {
					currVector.set( Value.createLink( rightPath ), index );
				}
			}
		}
	}
	
	private Object getValueOrValueVector()
	{	
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		for( int i = 0; i < path.length; i++ ) {
			pair = path[i];
			currVector = currValue.getChildren( pair.key().evaluate().strValue() );
			if ( pair.value() == null ) {
				if ( (i+1) < path.length ) {
					currValue = currVector.get( 0 );
				} else { // We're finished
					return currVector;
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( (i+1) < path.length ) {
					currValue = currVector.get( index );
				} else {
					Value ret = currVector.get( index );
					return ret;
				}
			}
		}

		return currValue;
	}
	
	@SuppressWarnings("unchecked")
	public void deepCopy( VariablePath rightPath )
	{
		Object myObj = getValueOrValueVector();
		if ( myObj instanceof Value ) {
			Value myVal = (Value) myObj;
			myVal.deepCopy( rightPath.getValue() );
		} else {
			ValueVector myVec = (ValueVector) myObj;
			ValueVector rightVec = rightPath.getValueVector();
			for( int i = 0; i < rightVec.size(); i++ ) {
				myVec.get( i ).deepCopy( rightVec.get( i ) );
			}
		}
	}
	
	public Value evaluate()
	{
		final Value v = getValueOrNull();
		if ( v == null )
			return Value.UNDEFINED_VALUE;
		return v;
	}
}
