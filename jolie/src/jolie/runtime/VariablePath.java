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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.process.TransformationReason;
import jolie.util.Pair;

public class VariablePath implements Expression, Cloneable
{
	private List< Pair< Expression, Expression > > path; // Expression may be null
	private Expression attribute; // may be null
	private boolean global;
	
	public VariablePath clone()
	{
		List< Pair< Expression, Expression > > list =
			new Vector< Pair< Expression, Expression > >();
		for( Pair< Expression, Expression > p : path )
			list.add( new Pair< Expression, Expression >( p.key(), p.value() ) );
		return new VariablePath( list, attribute, global );
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		List< Pair< Expression, Expression > > list =
			new Vector< Pair< Expression, Expression > >();
		for( Pair< Expression, Expression > p : path )
			list.add( new Pair< Expression, Expression >( p.key().cloneExpression( reason ), ( p.value() == null ) ? null : p.value().cloneExpression( reason ) ) );
		return new VariablePath( list, ( attribute == null ) ? null : attribute.cloneExpression( reason ), global );
	}

	/**
	 * @param otherVarPath
	 * @return
	 */
	public VariablePath containedSubPath( VariablePath otherVarPath )
	{
		// If one is global and the other is not, it's not a subpath.
		if ( global != otherVarPath.global )
			return null;

		// If the other path is shorter than this, it's not a subpath.
		if ( otherVarPath.path.size() < path.size() )
			return null;

		int i, myIndex, otherIndex;
		Pair< Expression, Expression > pair, otherPair;
		Expression expr, otherExpr;
		for( i = 0; i < path.size(); i++ ) {
			pair = path.get( i );
			otherPair = otherVarPath.path.get( i );
			
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
		for( ; i < otherVarPath.path.size(); i++ )
			subPath.add( otherVarPath.path.get( i ) );
		
		return new VariablePath( subPath, null, global );
	}
	
	public void addPathNode( Expression nodeExpr, Expression expression )
	{
		path.add( new Pair< Expression, Expression >( nodeExpr, expression ) );
	}
	
	public VariablePath(
			List< Pair< Expression, Expression > > path,
			Expression attribute,
			boolean global
			)
	{
		this.path = path;
		this.attribute = attribute;
		this.global = global;
	}
	
	private Value getRootValue()
	{
		if ( global )
			return Interpreter.getInstance().globalValue();
		
		return ExecutionThread.currentThread().state().root();
	}
	
	public void undef()
	{
		Iterator< Pair< Expression, Expression > > it = path.iterator();
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			String keyStr = pair.key().evaluate().strValue();
			currVector = currValue.children().get( keyStr );
			if ( currVector == null || currVector.size() < 1 )
				return;
			if ( pair.value() == null ) {
				if ( it.hasNext() ) {
					currValue = currVector.first();
				} else { // We're finished
					if ( attribute == null ) {
						currValue.children().remove( keyStr );
					} else {
						currVector.first().attributes().remove( attribute.evaluate().strValue() );
					}
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( it.hasNext() ) {
					if ( currVector.size() <= index )
						return;
					currValue = currVector.get( index );
				} else {
					if ( attribute == null ) {
						if ( currVector.size() > index )
							currVector.remove( index );
					} else {
						if ( currVector.size() > index )
							currVector.get( index ).attributes().remove( attribute.evaluate().strValue() );
					}
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

		for( Pair< Expression, Expression > pair : path ) {
			String keyStr = pair.key().evaluate().strValue();
			if ( pair.value() == null )
				currValue =
					currValue.getChildren( keyStr ).first();
			else
				currValue =
					currValue.getChildren( keyStr ).get( pair.value().evaluate().intValue() );
		}
		
		if ( attribute == null )
			return currValue;
		
		return currValue.getAttribute( attribute.evaluate().strValue() );
	}
	
	public Value getValueOrNull()
	{
		Iterator< Pair< Expression, Expression > > it = path.iterator();
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.children().get( pair.key().evaluate().strValue() );
			if ( currVector == null )
				return null;
			if ( pair.value() == null ) {
				if ( it.hasNext() ) {
					if ( currVector.size() < 1 )
						return null;
					currValue = currVector.first();
				} else { // We're finished
					if ( currVector.size() < 1 )
						return null;
					if ( attribute == null ) {
						return currVector.first();
					} else {
						if ( currVector.size() < 1 )
							return null;
						return currVector.first().attributes().get( attribute.evaluate().strValue() );
					}
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( currVector.size() <= index )
					return null;
				currValue = currVector.get( index );
				if ( !it.hasNext() ) {
					if ( attribute == null ) {
						return currValue;
					} else {
						return currValue.attributes().get( attribute.evaluate().strValue() );
					}
				}
			}
		}

		return null;
	}
	
	public ValueVector getValueVector()
	{
		Iterator< Pair< Expression, Expression > > it = path.iterator();
		Pair< Expression, Expression > pair;
		ValueVector currVector = null;
		Value currValue = getRootValue();

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.getChildren( pair.key().evaluate().strValue() );
			if ( it.hasNext() ) {
				if ( pair.value() != null ) {
					currValue = currVector.get( pair.value().evaluate().intValue() );
				} else {
					currValue = currVector.first();
				}
			}
		}

		return currVector;
	}
	
	public void makePointer( VariablePath rightPath )
	{
		Iterator< Pair< Expression, Expression > > it = path.iterator();
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			String keyStr = pair.key().evaluate().strValue();
			currVector = currValue.getChildren( keyStr );
			if ( pair.value() == null ) {
				if ( it.hasNext() ) {
					currValue = currVector.first();
				} else { // We're finished
					if ( attribute == null ) {
						currValue.children().put( keyStr, ValueVector.createLink( rightPath ) );
					} else {
						currVector.first().attributes().put( attribute.evaluate().strValue(), Value.createLink( rightPath ) );
					}
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( it.hasNext() ) {
					currValue = currVector.get( index );
				} else {
					if ( attribute == null ) {
						currVector.set( Value.createLink( rightPath ), index );
					} else {
						currVector.get( index ).attributes().put( attribute.evaluate().strValue(), Value.createLink( rightPath ) );
					}
				}
			}
		}
	}
	
	private Object getValueOrValueVector()
	{
		Iterator< Pair< Expression, Expression > > it = path.iterator();
		Pair< Expression, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.getChildren( pair.key().evaluate().strValue() );
			if ( pair.value() == null ) {
				if ( it.hasNext() ) {
					currValue = currVector.first();
				} else { // We're finished
					if ( attribute == null ) {
						return currVector;
					} else {
						return currVector.first().getAttribute( attribute.evaluate().strValue() );
					}
				}
			} else {
				index = pair.value().evaluate().intValue();
				if ( it.hasNext() ) {
					currValue = currVector.get( index );
				} else {
					if ( attribute == null ) {
						return currVector.get( index );
					} else {
						return currVector.get( index ).getAttribute( attribute.evaluate().strValue() );
					}
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
		Value v = getValueOrNull();
		if ( v == null )
			return Value.create();
		return v.evaluate();
	}
}
