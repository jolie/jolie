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


package jolie.process;

import jolie.AbstractMappedGlobalObject;
import jolie.Interpreter;
import jolie.InvalidIdException;

public class DefinitionProcess extends AbstractMappedGlobalObject implements Process, Optimizable
{
	private Process process;

	public DefinitionProcess( String id, Process process )
	{
		super( id );
		this.process = process;
	}

	public void setProcess( Process process )
	{
		this.process = process;
	}

	public void run()
	{
		if ( process != null )
			process.run();
	}

	public static DefinitionProcess getById( String id )
		throws InvalidIdException
	{
		Object obj = Interpreter.getObjectById( id );
		if ( !( obj instanceof DefinitionProcess ) )
			throw new InvalidIdException( id );
		return (DefinitionProcess) obj;
	}
	
	public Process optimize()
	{
		if ( process != null && process instanceof Optimizable )
			return ((Optimizable)process).optimize();
		
		return this;
	}
}
