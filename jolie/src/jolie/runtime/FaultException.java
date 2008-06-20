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

import java.io.ByteArrayOutputStream;

import java.io.PrintStream;
import jolie.Constants;

public class FaultException extends Exception
{
	private static final long serialVersionUID = Constants.serialVersionUID();
	final private String faultName;
	final private Value value;
	
	public FaultException( String faultName, Throwable t )
	{
		super();
		this.faultName = faultName;
		this.value = Value.create( t.getMessage() );
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		t.printStackTrace( new PrintStream( bs ) );
		this.value.getNewChild( "stackTrace" ).setValue( bs.toString() );
	}
	
	public FaultException( Throwable t )
	{
		super();
		this.faultName = t.getClass().getName();
		this.value = Value.create( t.getMessage() );
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		t.printStackTrace( new PrintStream( bs ) );
		this.value.getNewChild( "stackTrace" ).setValue( bs.toString() );
	}
	
	public FaultException( String faultName, String message )
	{
		super();
		this.faultName = faultName;
		this.value = Value.create( message );
	}
	
	public FaultException( String faultName, Value value )
	{
		super();
		this.faultName = faultName;
		this.value = value;
	}
	
	public FaultException( String faultName )
	{
		super();
		this.faultName = faultName;
		this.value = Value.create();
	}
	
	@Override
	public String getMessage()
	{
		return value.strValue();
	}
	
	public Value value()
	{
		return value;
	}
	
	public String faultName()
	{
		return faultName;
	}
}
