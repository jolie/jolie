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

import jolie.runtime.FaultException;
import jolie.runtime.Value;

/**
 * @author Fabrizio Montesi
 * @todo Extend to subclasses with a direct reference to the input object, for performance improvements?
 *
 */
public class CommMessage
{
	private String inputId;
	private Value value;
	private boolean fault = false;

	public static CommMessage createEmptyMessage()
	{
		return new CommMessage( "" );
	}
	
	public CommMessage( String inputId, Value value )
	{
		this.inputId = inputId;
		this.value = value;
	}
	
	public CommMessage( String inputId )
	{
		this.inputId = inputId;
		this.value = Value.create();
	}

	public CommMessage( String inputId, FaultException f )
	{
		this.inputId = inputId;
		this.value = Value.create( f.fault() );
		fault = true;
	}
	
	public Value value()
	{
		return value;
	}
	
	public String inputId()
	{
		return inputId;
	}

	public boolean isFault()
	{
		return fault;
	}
	
	public String faultName()
	{
		return value.strValue();
	}
}