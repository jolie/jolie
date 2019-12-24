/***************************************************************************
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie.tracer;

import jolie.lang.parse.context.ParsingContext;
import jolie.net.CommMessage;

/**
 *
 * @author Fabrizio Montesi
 */
public class MessageTraceAction implements TraceAction
{
	public static enum Type {
		SOLICIT_RESPONSE,
		NOTIFICATION,
		ONE_WAY,
		REQUEST_RESPONSE,
		COURIER_NOTIFICATION,
		COURIER_SOLICIT_RESPONSE
	}
	
	private final Type type;
	private final String name;
	private final String description;
	private final CommMessage message;
	private final ParsingContext context;
	
	public MessageTraceAction(Type type, String name, String description, CommMessage message, ParsingContext context )
	{
		this.type = type;
		this.name = name;
		this.description = description;
		this.message = message;
		this.context = context;
	}

	public Type type()
	{
		return type;
	}
	
	public String name()
	{
		return name;
	}
	
	public String description()
	{
		return description;	
	}
	
	public CommMessage message()
	{
		return message;
	}

	public ParsingContext context() { return context; }
}
