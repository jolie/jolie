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

public class JavaCommChannel extends CommChannel
{
	private JavaService javaService;
	private CommMessage lastMessage = null;

	public JavaCommChannel( JavaService javaService )
	{
		this.javaService = javaService;
	}
	
	public void send( CommMessage message )
	{
		if ( javaService != null )
			lastMessage = javaService.recv( message );
	}
	
	public CommMessage recv()
	{
		CommMessage ret = lastMessage;
		lastMessage = null;
		return ret;
	}
	
	public void close()
	{
		javaService = null;
	}
}
