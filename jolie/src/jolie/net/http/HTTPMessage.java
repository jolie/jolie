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

package jolie.net.http;

import java.io.CharArrayReader;


public class HTTPMessage
{
	public enum Type {
		RESPONSE, POST, GET, ERROR
	}
	
	private Type type;
	private char[] content;
	private int httpCode;
	private String requestPath;

	public HTTPMessage( int httpCode, char[] content )
	{
		this.httpCode = httpCode;
		this.content = content;
		this.type = Type.RESPONSE;
	}
	
	public HTTPMessage( Type type, String requestPath, char[] content )
	{
		this.type = type;
		this.content = content;
		this.requestPath = requestPath;
	}
	
	public int size()
	{
		return content.length;
	}
	
	public String requestPath()
	{
		return requestPath;
	}
	
	public Type type()
	{
		return type;
	}
	
	public int httpCode()
	{
		return httpCode;
	}
	
	public CharArrayReader contentStream()
	{
		return new CharArrayReader( content );
	}
}
