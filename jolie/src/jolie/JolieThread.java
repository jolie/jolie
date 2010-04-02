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
 * A thread that can refer to its generating <code>Interpreter</code>.
 * The name of <code>JolieThread</code> is prefixed with the program file name
 * of the generating <code>Interpreter</code>.
 * @see Interpreter
 * @author Fabrizio Montesi
 */
public abstract class JolieThread extends Thread
{
	private final Interpreter interpreter;

	private static final AtomicInteger counter = new AtomicInteger( 0 );

	private static String createThreadName()
	{
		return "JolieThread-" + counter.getAndIncrement();
	}

	/**
	 * Constructor
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @param threadGroup the <code>ThreadGroup</code> for this thread
	 * @param name the suffix name for this thread
	 * @see Interpreter
	 * @see java.lang.Thread
	 * @see java.lang.ThreadGroup
	 */
	public JolieThread( Interpreter interpreter, ThreadGroup threadGroup, String name )
	{
		super( threadGroup, interpreter.programFilename() + "-" + name );
		this.interpreter = interpreter;
	}

	/**
	 * Constructor
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @see Interpreter
	 * @see java.lang.Thread
	 * @see java.lang.ThreadGroup
	 */
	public JolieThread( Interpreter interpreter )
	{
		super( interpreter.programFilename() + "-" + createThreadName() );
		this.interpreter = interpreter;
	}

	/**
	 * Constructor
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @see Interpreter
	 * @see java.lang.Thread
	 * @see java.lang.ThreadGroup
	 */
	public JolieThread( Interpreter interpreter, Runnable r )
	{
		super( r, interpreter.programFilename() + "-" + createThreadName() );
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
