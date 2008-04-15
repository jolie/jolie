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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import jolie.Constants;

public class FaultException extends Exception
{
	private static final long serialVersionUID = Constants.serialVersionUID();
	private String faultName;
	private Value value;
	
	public FaultException() {}
	
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
	
	public static FaultException createFromExternal( DataInput in )
		throws IOException, ClassNotFoundException
	{
		FaultException f = null;
		if ( in.readBoolean() ) {
			f = new FaultException();
			f.readExternal( in );
		}
		return f;
	}
	
	public void readExternal( DataInput in )
		throws IOException, ClassNotFoundException
	{
		faultName = in.readUTF();
		value = Value.createFromExternal( in );
	}
	
	public void writeExternal( DataOutput out )
		throws IOException
	{
		out.writeUTF( faultName );
		value.writeExternal( out );
	}
	
	/*public FaultException( Exception e )
	{
		super( e );
	}*/
	
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
