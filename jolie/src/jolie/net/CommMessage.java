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
 * 
 * @author Fabrizio Montesi
 *
 */
public class CommMessage
{
	private static final long serialVersionUID = 1L;
	
	final private String operationName;
	final private String resourcePath;
	final private Value value;
	final private FaultException fault;
	
	public static CommMessage createEmptyMessage()
	{
		return new CommMessage( "", "/" );
	}
	
	public String resourcePath()
	{
		return resourcePath;
	}
	
	public CommMessage( String operationName, String resourcePath, Value value )
	{
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		// TODO This is a performance hit! Make this only when strictly necessary.
		// Perhaps let it be handled by CommProtocol and/or CommChannel ?  
		this.value = Value.createDeepCopy( value );
		this.fault = null;
	}
	
	public CommMessage( String operationName, String resourcePath, FaultException f )
	{
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		this.value = Value.create();
		this.fault = f;
	}
	
	public CommMessage( String operationName, String resourcePath, Value value, FaultException f )
	{
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		// TODO see above performance hit.
		this.value = Value.createDeepCopy( value );
		fault = f;
	}
	
	public CommMessage( String inputId, String resourcePath )
	{
		this.operationName = inputId;
		this.resourcePath = resourcePath;
		this.value = Value.create();
		this.fault = null;
	}
	
	public Value value()
	{
		return value;
	}
	
	public String operationName()
	{
		return operationName;
	}

	public boolean isFault()
	{
		return ( fault != null );
	}
	
	public FaultException fault()
	{
		return fault;
	}
}