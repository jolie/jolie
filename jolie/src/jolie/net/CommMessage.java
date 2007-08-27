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


package jolie.net;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class CommMessage implements Iterable< Value >
{
	private String inputId;
	private Vector< Value > values;
	private boolean fault = false;
	
	public CommMessage()
	{
		inputId = "";
		values = new Vector< Value >();
	}
	
	public CommMessage( String inputId )
	{
		this.inputId = inputId;
		this.values = new Vector< Value >();
	}
	
	public CommMessage( String inputId, FaultException f )
	{
		this.inputId = inputId;
		this.values = new Vector< Value >();
		this.values.add( new Value( f.fault() ) );
		fault = true;
	}
	
	public CommMessage( String inputId, Vector< ? extends Value > values )
	{
		this.inputId = inputId;
		this.values = new Vector< Value >();
		addAllValues( values );
	}

	public String inputId()
	{
		return inputId;
	}

	public void addValue( Value val )
	{
		values.add( val );
	}
	
	public final void addAllValues( Collection< ? extends Value > vals )
	{
		for( Value val : vals )
			addValue( val );
	}
	
	public Iterator< Value > iterator()
	{
		return values.iterator();
	}
	
	public int size()
	{
		return values.size();
	}
	
	public boolean isFault()
	{
		return fault;
	}
	
	public String faultName()
	{
		return values.elementAt( 0 ).strValue();
	}
}