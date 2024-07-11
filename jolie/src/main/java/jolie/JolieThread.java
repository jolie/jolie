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
 * A thread that can refer to its generating <code>Interpreter</code>. The name of
 * <code>JolieThread</code> is prefixed with the program file name of the generating
 * <code>Interpreter</code>.
 *
 * @see Interpreter
 * @author Fabrizio Montesi
 */
public abstract class JolieThread implements Runnable {
	private final Interpreter interpreter;
	private final String name;
	private static final AtomicInteger COUNTER = new AtomicInteger( 0 );

	protected static String createThreadName() {
		return "JolieThread-" + COUNTER.getAndIncrement();
	}

	public String name() {
		return name;
	}

	/**
	 * Constructor
	 *
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @param name the suffix name for this thread
	 * @see Interpreter
	 */
	public JolieThread( Interpreter interpreter, String name ) {
		this.interpreter = interpreter;
		this.name = interpreter.programFilename() + "-" + name;
	}

	/**
	 * Constructor
	 *
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @see Interpreter
	 */
	public JolieThread( Interpreter interpreter ) {
		this( interpreter, createThreadName() );
	}

	/**
	 * Returns the interpreter that this thread refers to.
	 */
	public Interpreter interpreter() {
		return interpreter;
	}
}
