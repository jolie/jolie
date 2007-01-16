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


package jolie;

import java.io.IOException;
import java.net.Socket;

import jolie.net.CommChannel;
import jolie.net.CommProtocol;

abstract public class Location
{
	abstract public String value();
	abstract public void setValue( String value );
	
	public CommChannel createCommChannel( CommProtocol protocol )
		throws IOException
	{
		String[] tokens = value().split( ":" );
		int port = Integer.parseInt( tokens[ 1 ] );

		Socket socket = new Socket( tokens[0], port );
		return new CommChannel( socket.getInputStream(), socket.getOutputStream(), protocol );
	}
}