/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

import java.lang.ref.WeakReference;

public class NativeJolieThread extends Thread implements InterpreterThread {
	private final WeakReference< Interpreter > interpreter;

	/**
	 * Constructor
	 */
	public NativeJolieThread( Interpreter interpreter, ThreadGroup group, String name ) {
		super( group, interpreter.programFilename() + "-" + name );
		this.interpreter = new WeakReference<>( interpreter );
	}

	/**
	 * Constructor
	 *
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @param name the suffix name for this thread
	 * @see Interpreter
	 */
	public NativeJolieThread( Interpreter interpreter, String name ) {
		super( interpreter.programFilename() + "-" + name );
		this.interpreter = new WeakReference<>( interpreter );
	}

	/**
	 * Constructor
	 *
	 * @param interpreter the <code>Interpreter</code> this thread will refer to
	 * @see Interpreter
	 */
	public NativeJolieThread( Interpreter interpreter ) {
		this( interpreter, JolieThread.createThreadName() );
	}

	/**
	 * Constructor
	 */
	public NativeJolieThread( Interpreter interpreter, Runnable r ) {
		super( r, interpreter.programFilename() + "-" + JolieThread.createThreadName() );
		this.interpreter = new WeakReference<>( interpreter );
	}

	/**
	 * Returns the interpreter that this thread refers to.
	 */
	@Override
	public Interpreter interpreter() {
		return interpreter.get();
	}

	/**
	 * Clear interpreter reference
	 */
	public void clearInterpreter() {
		interpreter.clear();
	}
}
