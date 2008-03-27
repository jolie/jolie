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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import jolie.runtime.FaultException;
import jolie.runtime.Value;

/**
 * @author Fabrizio Montesi
 *
 */
public class CommMessage implements Externalizable
{
	private static final long serialVersionUID = 1L;
	
	private String inputId;
	private Value value;
	private boolean fault = false;
	
	public CommMessage() {}
	
	public static CommMessage createFromExternal( ObjectInput in )
		throws IOException, ClassNotFoundException
	{
		CommMessage m = new CommMessage();
		m.readExternal( in );
		return m;
	}
	
	public void readExternal( ObjectInput in )
		throws IOException, ClassNotFoundException
	{
		inputId = in.readUTF();
		fault = in.readBoolean();
		value = Value.createFromExternal( in );
	}
	
	public void writeExternal( ObjectOutput out )
		throws IOException
	{
		out.writeUTF( inputId );
		out.writeBoolean( fault );
		value.writeExternal( out );
	}
	
	public static CommMessage createEmptyMessage()
	{
		return new CommMessage( "" );
	}
	
	public CommMessage( String inputId, Value value )
	{
		this.inputId = inputId;
		// TODO This is a performance hit! Make this only when strictly necessary.
		// Perhaps let it be handled by CommProtocol and/or CommChannel ?  
		this.value = Value.createDeepCopy( value );
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