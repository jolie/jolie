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
import java.nio.channels.Channel;




/** A communication channel permits to send and receive messages.
 * 
 * @author Fabrizio Montesi
 * @see CommProtocol
 * @see CommMessage
 */
abstract public class CommChannel implements Channel
{
	protected boolean toBeClosed = true;
	private CommListener listener = null;
	private boolean isOpen = false;
	
	private CommChannel redirectionChannel = null;
	
	public void setRedirectionChannel( CommChannel redirectionChannel )
	{
		this.redirectionChannel = redirectionChannel;
	}
	
	public CommChannel redirectionChannel()
	{
		return redirectionChannel;
	}
	
	public void refreshProtocol()
	{}
	
	public void setParentListener( CommListener listener )
	{
		this.listener = listener;
	}
	
	public CommListener parentListener()
	{
		return listener;
	}
	
	final public boolean isOpen()
	{
		return isOpen;
	}
	
	/** Receives a message from the channel. */
	abstract public CommMessage recv()
		throws IOException;
	
	/** Sends a message through the channel. */
	abstract public void send( CommMessage message )
		throws IOException;
	
	/** Closes the communication channel */
	final public void close()
		throws IOException
	{
		if ( toBeClosed ) {
			closeImpl();
			isOpen = false;
		}
	}
	
	final public void disposeForInput()
		throws IOException
	{
		if ( toBeClosed )
			closeImpl();
		else
			disposeForInputImpl();
	}
	
	protected void disposeForInputImpl() {}
	
	public void setToBeClosed( boolean toBeClosed )
	{
		this.toBeClosed = toBeClosed;
	}

	/** Implements the communication channel closing operation. */
	abstract protected void closeImpl()
		throws IOException;
}