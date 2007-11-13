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

class ValueVectorImpl extends ValueVector
{
	private Vector< Value > values;
	
	public boolean isLink()
	{
		return false;
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
	
	protected void deepCopy( ValueVector vec, boolean copyLinks )
	{
		/*for( int i = 0; i < vec.size(); i++ )
			values.elementAt( i ).deepCopy( vec.elementAt( i ) );*/
	}
}

abstract public class ValueVector implements Iterable< Value >
{
	abstract public boolean isLink();
	
	abstract public int size();
	
	//abstract public Value elementAt
	
	//abstract protected void _deepCopy( ValueVector vec, boolean copyLinks );
	
	abstract protected void deepCopy( ValueVector vec, boolean copyLinks );
	//abstract protected void deepClone( ValueVector vec, boolean copyLinks );
}
