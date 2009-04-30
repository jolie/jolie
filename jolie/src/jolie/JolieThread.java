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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread that can refer to its generating Interpreter.
 * @see Interpreter
 * @author Fabrizio Montesi
 */
abstract public class JolieThread extends Thread
{
	final private Interpreter interpreter;

	static final private AtomicInteger counter = new AtomicInteger( 0 );

	static private String createThreadName()
	{
		return "JolieThread-" + counter.getAndIncrement();
	}
	
	public JolieThread( Interpreter interpreter, ThreadGroup threadGroup, String name )
	{
		super( threadGroup, interpreter.programFile().getName() + "-" + name );
		this.interpreter = interpreter;
	}
	
	public JolieThread( Interpreter interpreter )
	{
		super( interpreter.programFile().getName() + "-" + createThreadName() );
		this.interpreter = interpreter;
	}
	
	public JolieThread( Interpreter interpreter, Runnable r )
	{
		super( r, interpreter.programFile().getName() + "-" + createThreadName() );
		this.interpreter = interpreter;
	}
	
	/**
	 * Returns the interpreter that this thread refers to.
	 */
	public Interpreter interpreter()
	{
		return interpreter;
	}
}
