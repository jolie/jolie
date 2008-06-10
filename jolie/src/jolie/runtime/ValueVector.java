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
import java.util.Vector;
import jolie.ExecutionThread;
import jolie.Interpreter;

class ValueVectorLink extends ValueVector implements Cloneable
{
	final private VariablePath linkPath;
	
	// We need this in order to distinguish links in embedded interpreters.
	final private Value rootValue;

	public Value remove( int i )
	{
		return getLinkedValueVector().remove( i );
	}
	
	@Override
	public ValueVectorLink clone()
	{
		return new ValueVectorLink( linkPath, rootValue );
	}

	public ValueVectorLink( VariablePath path, Value rootValue )
	{
		linkPath = path;
		this.rootValue = rootValue;
	}

	public boolean isLink()
	{
		return true;
	}
	
	private ValueVector getLinkedValueVector()
	{
		return linkPath.getValueVector( rootValue );
	}
	
	public Value get( int i )
	{
		return getLinkedValueVector().get( i );
	}
	
	public void deepCopy( ValueVector vec )
	{
		getLinkedValueVector().deepCopy( vec );
	}
	
	public Iterator< Value > iterator()
	{
		return getLinkedValueVector().iterator();
	}
	
	public int size()
	{
		return getLinkedValueVector().size();
	}
	
	public void add( Value value )
	{
		getLinkedValueVector().add( value );
	}
	
	public void set( Value value, int i )
	{
		getLinkedValueVector().set( value, i );
	}
}

class ValueVectorImpl extends ValueVector
{
	final private Vector< Value > values;
	
	public boolean isLink()
	{
		return false;
	}
	
	public Value remove( int i )
	{
		return values.remove( i );
	}
	
	public ValueVectorImpl()
	{
		values = new Vector< Value >();
	}
	
	public Iterator< Value > iterator()
	{
		return values.iterator();
	}
	
	public int size()
	{
		return values.size();
	}
	
	public Value get( int i )
	{
		if ( i >= values.size() ) {
			for( int k = values.size(); k <= i; k++ )
				values.add( Value.create() );
		}
		return values.elementAt( i );
	}
	
	public void set( Value value, int i )
	{
		if ( i >= values.size() ) {
			for( int k = values.size(); k < i; k++ )
				values.add( Value.create() );
			values.add( value );
		} else {
			values.set( i, value );
		}
	}
	
	public void deepCopy( ValueVector vec )
	{
		for( int i = 0; i < vec.size(); i++ )
			get( i ).deepCopy( vec.get( i ) );
	}
	
	public void add( Value value )
	{
		values.add( value );
	}
}

abstract public class ValueVector implements Iterable< Value >
{	
	public static ValueVector create()
	{
		return new ValueVectorImpl();
	}
	
	abstract public Value remove( int i );
	
	public static ValueVector createLink( VariablePath path )
	{
		Value rootValue;
		if ( path.isGlobal() ) {
			rootValue = Interpreter.getInstance().globalValue();
		} else {
			rootValue = ExecutionThread.currentThread().state().root();
		}
		return new ValueVectorLink( path, rootValue );
	}
	
	public static ValueVector createClone( ValueVector vec )
	{
		ValueVector retVec = null;
		
		if ( vec.isLink() ) {
			retVec = ((ValueVectorLink)vec).clone();
		} else {
			retVec = create();
			for( Value v : vec )
				retVec.add( Value.createClone( v ) );
		}
		
		return retVec;
	}
	
	public Value first()
	{
		return get( 0 );
	}
	
	public boolean isEmpty()
	{
		return ( size() < 1 );
	}
	
	abstract public void add( Value value );
	abstract public boolean isLink();
	abstract public int size();
	abstract public Value get( int i );
	abstract public void set( Value value, int i );
	abstract public void deepCopy( ValueVector vec );
}
