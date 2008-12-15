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
	
	private static long idCounter = 1L;
	final private static Object idCounterMutex = new Object();
	
	public static final long GENERIC_ID = 0L;
	
	final private long id;
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
	
	public boolean hasGenericId()
	{
		return id == GENERIC_ID;
	}
	
	public long id()
	{
		return id;
	}

	private static long getNewMessageId()
	{
		synchronized( idCounterMutex ) {
			return idCounter++;
		}
	}
	
	public static CommMessage createRequest( String operationName, String resourcePath, Value value )
	{
		return new CommMessage( getNewMessageId(), operationName, resourcePath, value );
	}
	
	public static CommMessage createResponse( CommMessage request, Value value )
	{
		//TODO support resourcePath
		return new CommMessage( request.id, request.operationName, "/", value );
	}
	
	public static CommMessage createFaultResponse( CommMessage request, FaultException fault )
	{
		//TODO support resourcePath
		return new CommMessage( request.id, request.operationName, "/", fault );
	}
	
	private CommMessage( long id, String operationName, String resourcePath, Value value )
	{
		this.id = id;
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		// TODO This is a performance hit! Make this only when strictly necessary.
		// Perhaps let it be handled by CommProtocol and/or CommChannel ?  
		this.value = Value.createDeepCopy( value );
		this.fault = null;
	}
	
	private CommMessage( long id, String operationName, String resourcePath, FaultException fault )
	{
		this.id = id;
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		this.value = Value.create();
		this.fault = fault;
	}
	
	public CommMessage( String operationName, String resourcePath, Value value )
	{
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		// TODO This is a performance hit! Make this only when strictly necessary.
		// Perhaps let it be handled by CommProtocol and/or CommChannel ?  
		this.value = Value.createDeepCopy( value );
		this.fault = null;
		this.id = 0L;
	}
	
	public CommMessage( long id, String operationName, String resourcePath, Value value, FaultException f )
	{
		this.id = id;
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		// TODO see above performance hit.
		this.value = Value.createDeepCopy( value );
		fault = f;
	}
	
	public CommMessage( String operationName, String resourcePath, Value value, FaultException f )
	{
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		// TODO see above performance hit.
		this.value = Value.createDeepCopy( value );
		fault = f;
		this.id = 0L;
	}
	
	public CommMessage( String operationName, String resourcePath )
	{
		this.operationName = operationName;
		this.resourcePath = resourcePath;
		this.value = Value.create();
		this.fault = null;
		this.id = 0L;
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