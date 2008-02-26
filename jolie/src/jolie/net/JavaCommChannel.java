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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jolie.runtime.FaultException;
import jolie.runtime.JavaService;

public class JavaCommChannel extends CommChannel
{
	private Object javaService;
	private CommMessage lastMessage = null;
	
	public JavaCommChannel( JavaService javaService )
	{
		this.javaService = javaService;
	}
	
	public void send( CommMessage message )
		throws IOException
	{
		lastMessage = null;
		if ( javaService != null ) {
			try {
				Class<?>[] params = new Class[] { CommMessage.class };
				Method method = javaService.getClass().getMethod(
						message.inputId(),
						params
						);
				Object[] args = new Object[] { message };
				if ( CommMessage.class.isAssignableFrom( method.getReturnType() ) ) {
					// It's a Request-Response
					try {
						lastMessage = (CommMessage)method.invoke( javaService, args );
					} catch( InvocationTargetException ite ) {
						Throwable t = ite.getCause();
						if ( t instanceof FaultException ) {
							// The operation threw a fault
							FaultException f = (FaultException) t;
							lastMessage = new CommMessage(
										message.inputId(),
										f
										);
						} else {
							// The operation raised an exception
							throw new IOException( t );
						}
					}
				} else {
					// @todo Verify that it is void
					// It's a One-Way
					try {
						method.invoke( javaService, args );
					} catch( InvocationTargetException ite ) {
						throw new IOException( ite.getCause() );
					}
				}
			} catch( NoSuchMethodException noe ) {
				throw new IOException( noe );
			} catch( IllegalAccessException iae ) {
				throw new IOException( iae );
			}
		}
	}

	public CommMessage recv()
		throws IOException
	{
		if ( lastMessage == null )
			return CommMessage.createEmptyMessage();
		CommMessage ret = lastMessage;
		lastMessage = null;
		return ret;
	}

	public void close()
	{
		//javaService = null;
	}
	
	public boolean hasData()
	{
		return( lastMessage != null );
	}
}
