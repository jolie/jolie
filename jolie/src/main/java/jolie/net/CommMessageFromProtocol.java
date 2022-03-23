/***************************************************************************
 *   Copyright (C) by Mauro Sgarzi                                     *
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

import jolie.monitoring.events.ProtocolMessageEvent;
import jolie.net.CommMessage;

/**
 * A <code>CommMessageProtocol</code> represents a generic communication message + an protocolEvent
 *
 * Is used to bring around, for the receive operation the CommMessage and the ProtocolEvent used to
 * log if monitoring is enabled.
 *
 * @author Mauro Sgarzi
 */
public class CommMessageFromProtocol {
	private CommMessage message;
	private ProtocolMessageEvent protocolEvent;

	/**
	 * Returns the message field
	 * 
	 * @return <code>CommMessage<code>
	 */
	public CommMessage getMessage() {
		return message;
	}

	/**
	 * Set the message field
	 * 
	 * @return <code>void<code>
	 */
	public void setMessage( CommMessage msg ) {
		message = msg;
	}

	/**
	 * Returns the protocolEvent field
	 * 
	 * @return <code>ProtocolMessageEvent<code>
	 */
	public ProtocolMessageEvent getMessageEvent() {
		return protocolEvent;
	}

	/**
	 * Set the protocolEvent field
	 * 
	 * @return void
	 */
	public void setMessageEvent( ProtocolMessageEvent event ) {
		protocolEvent = event;
	}

	public CommMessageFromProtocol( CommMessage msg, ProtocolMessageEvent event ) {
		message = msg;
		protocolEvent = event;
	}
}
