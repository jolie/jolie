/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

import java.io.IOException;

/** CommChannelHandler is used by the communication core to handle a newly opened 
 * communication channel.
 * CommChannelHandler objects are grouped in a ThreadGroup, in order to be able 
 * to interrupt them in case of network shutdown.
 * 
 */
public class CommChannelHandler extends Thread
{
	private static int index = 0;
	
	private CommChannel channel;
	
	/** Constructor.
	 * 
	 * @param threadGroup the threadGroup to which the CommChannelHandler thread will be assigned.
	 * @param channel the channel to be handled.
	 */
	public CommChannelHandler( ThreadGroup threadGroup, CommChannel channel )
	{
		super( threadGroup, "CommChannelHandler-" + index );
		this.channel = channel;
	}

	public CommChannel commChannel()
	{
		return channel;
	}
	
	/** Runs the thread, making it waiting for a message.
	 * When a message is received, the thread creates a CommMessage object 
	 * and passes it to the relative InputOperation object, which will handle the
	 * received information.
	 * After the information has been sent to the InputOperation object, the 
	 * communication channel is closed and the thread terminates. 
	 */
	public void run()
	{
		try {
			CommMessage message = channel.recv();
			InputOperation operation =
					InputOperation.getById( message.inputId() );
			
			operation.recvMessage( message );
			
			channel.close();
		} catch( IOException ioe ) {
			// todo -- signal problem to CommCore?
			ioe.printStackTrace();
		} catch( InvalidIdException iie ) {
			iie.printStackTrace();
		}
	}
}