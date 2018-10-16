/**********************************************************************************
 *   Copyright (C) 2017-18 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017-18 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

package jolie.net.coap.message;

import java.util.HashMap;

public class MessageType
{

	public static final int CON = 0;
	public static final int NON = 1;
	public static final int ACK = 2;
	public static final int RST = 3;

	private static final HashMap<Integer, String> MESSAGE_TYPES
		= new HashMap<>();

	static {
		MESSAGE_TYPES.put( CON, "CON" );
		MESSAGE_TYPES.put( NON, "NON" );
		MESSAGE_TYPES.put( ACK, "ACK" );
		MESSAGE_TYPES.put( RST, "RST" );
	}

	/**
	 *
	 * @param messageType
	 * @return String
	 */
	public static String asString( int messageType )
	{
		String result = MESSAGE_TYPES.get( messageType );
		return result == null ? "UNKOWN (" + messageType + ")" : result;
	}

	/**
	 *
	 * @param number
	 * @return boolean
	 */
	public static boolean isMessageType( int number )
	{
		return MESSAGE_TYPES.containsKey( number );
	}

}
