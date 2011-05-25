/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

package jolie.net.http;

import jolie.net.CommChannel;

/**
 * Utilities for handling HTTP messages.
 * @author Fabrizio Montesi
 */
public class HttpUtils
{
	// Checks if the message requests the channel to be closed or kept open
	public static void recv_checkForChannelClosing( HttpMessage message, CommChannel channel )
	{
		if ( channel != null ) {
			HttpMessage.Version version = message.version();
			if ( version == null || version.equals( HttpMessage.Version.HTTP_1_1 ) ) {
				// The default is to keep the connection open, unless Connection: close is specified
				if ( message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "close" ) ) {
					channel.setToBeClosed( true );
				} else {
					channel.setToBeClosed( false );
				}
			} else if ( version.equals( HttpMessage.Version.HTTP_1_0 ) ) {
				// The default is to close the connection, unless Connection: Keep-Alive is specified
				if ( message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "keep-alive" ) ) {
					channel.setToBeClosed( false );
				} else {
					channel.setToBeClosed( true );
				}
			}
		}
	}
}
