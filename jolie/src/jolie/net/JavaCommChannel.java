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

import jolie.runtime.InvalidIdException;
import jolie.runtime.JavaService;

public class JavaCommChannel extends CommChannel
{
	final private JavaService javaService;
	private CommMessage lastMessage = null;
	
	final private Object[] args = new Object[1];
	
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
				lastMessage = javaService.callOperation( message );
			} catch( IllegalAccessException e ) {
				throw new IOException( e );
			} catch( InvalidIdException e ) {
				throw new IOException( e );
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

	protected void closeImpl()
	{}
	
	public boolean hasData()
	{
		return( lastMessage != null );
	}
}
