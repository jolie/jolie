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


package jolie.runtime;

import java.util.Vector;

import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.InternalCommChannel;


abstract public class JavaService
{
	private Interpreter interpreter;
	
	public void setInterpreter( Interpreter interpreter )
	{
		this.interpreter = interpreter;
	}
	
	protected CommChannel sendMessage( CommMessage message )
	{
		InternalCommChannel c = new InternalCommChannel(
							new Vector< CommMessage >(),
							new Vector< CommMessage >()
							);
		c.inputList().add( message );
		interpreter.commCore().scheduleReceive( c, null );
		return new InternalCommChannel( c.outputList(), c.inputList() );
	}
}