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

package jolie.runtime.embedding;

import jolie.runtime.JavaService;
import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.InvalidIdException;

/**
 * @author Fabrizio Montesi
 */
public class JavaCommChannel extends CommChannel
{
	final private JavaService javaService;
	final private List< CommMessage > messages = new LinkedList< CommMessage >();

	public JavaCommChannel( JavaService javaService )
	{
		this.javaService = javaService;
	}

	protected void sendImpl( CommMessage message )
		throws IOException
	{
		try {
			CommMessage response = javaService.callOperation( message );
			if ( response != null ) {
				response = new CommMessage(
							message.id(),
							message.operationName(),
							message.resourcePath(),
							response.value(),
							response.fault()
						);
				synchronized( messages ) {
					messages.add( response );
					messages.notifyAll();
				}
			}
		} catch( IllegalAccessException e ) {
			throw new IOException( e );
		} catch( InvalidIdException e ) {
			throw new IOException( e );
		}
	}

	protected CommMessage recvImpl()
		throws IOException
	{
		CommMessage ret = null;
		synchronized( messages ) {
			while( messages.isEmpty() ) {
				try {
					messages.wait();
				} catch( InterruptedException e ) {}
			}
			ret = messages.remove( 0 );
		}
		if ( ret == null ) {
			throw new IOException( "Unknown exception occurred during communications with a Java Service" );
		}
		return ret;
	}

	protected void closeImpl()
	{}
}
