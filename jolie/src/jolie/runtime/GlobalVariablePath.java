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
import jolie.util.Pair;

public class GlobalVariablePath implements Expression, Cloneable
{
	private List< Pair< String, Expression > > path; // Expression may be null
	private Expression attribute; // may be null
	private boolean global;
	
	public GlobalVariablePath clone()
	{
		List< Pair< String, Expression > > list =
			new Vector< Pair< String, Expression > >();
		for( Pair< String, Expression > p : path )
			list.add( new Pair< String, Expression >( p.key(), p.value() ) );
		return new GlobalVariablePath( list, attribute, global );
	}
	
	public void addPathNode( String nodeName, Expression expression )
	{
		path.add( new Pair< String, Expression >( nodeName, expression ) );
	}
	
	public GlobalVariablePath(
			List< Pair< String, Expression > > path,
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
			return Interpreter.globalValue();
		
		return ExecutionThread.currentThread().state().root();
	}
	
	public void undef()
	{
		Iterator< Pair< String, Expression > > it = path.iterator();
		Pair< String, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.children().get( pair.key() );
			if ( currVector == null || currVector.size() < 1 )
				return;
			if ( pair.value() == null ) {
				if ( it.hasNext() ) {
					currValue = currVector.first();
				} else { // We're finished
					if ( attribute == null ) {
						currValue.children().remove( pair.key() );
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
		Value currValue = getRootValue();

		for( Pair< String, Expression > pair : path ) {
			if ( pair.value() == null )
				currValue =
					currValue.getChildren( pair.key() ).first();
			else
				currValue =
					currValue.getChildren( pair.key() ).get( pair.value().evaluate().intValue() );
		}
		
		if ( attribute == null )
			return currValue;
		
		return currValue.getAttribute( attribute.evaluate().strValue() );
	}
	
	public Value getValueOrNull()
	{
		Iterator< Pair< String, Expression > > it = path.iterator();
		Pair< String, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.children().get( pair.key() );
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
		Iterator< Pair< String, Expression > > it = path.iterator();
		Pair< String, Expression > pair;
		ValueVector currVector = null;
		Value currValue = getRootValue();

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.getChildren( pair.key() );
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
	
	public void makePointer( GlobalVariablePath rightPath )
	{
		Iterator< Pair< String, Expression > > it = path.iterator();
		Pair< String, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.getChildren( pair.key() );
			if ( pair.value() == null ) {
				if ( it.hasNext() ) {
					currValue = currVector.first();
				} else { // We're finished
					if ( attribute == null ) {
						currValue.children().put( pair.key(), ValueVector.createLink( rightPath ) );
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
		Iterator< Pair< String, Expression > > it = path.iterator();
		Pair< String, Expression > pair = null;
		ValueVector currVector = null;
		Value currValue = getRootValue();
		int index;

		while( it.hasNext() ) {
			pair = it.next();
			currVector = currValue.getChildren( pair.key() );
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
	public void deepCopy( GlobalVariablePath rightPath )
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
