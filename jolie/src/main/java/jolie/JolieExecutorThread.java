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

/**
 *
 * @author Fabrizio Montesi
 */
public class JolieExecutorThread extends Thread implements InterpreterThread {
	private ExecutionThread executionThread;

	public JolieExecutorThread( Runnable r, Interpreter interpreter ) {
		super( r, interpreter.programFilename() + "-" + JolieThread.createThreadName() );
	}

	/**
	 * Sets the <code>ExecutionThread</code> this thread must refer to.
	 *
	 * @param thread the <code>ExecutionThread</code> this thread must refer to for variable state
	 *        resolution
	 */
	public final void setExecutionThread( ExecutionThread thread ) {
		executionThread = thread;
	}

	/**
	 * Returns the <code>ExecutionThread</code> this thread is referring to for variable state
	 * resolution.
	 *
	 * @return the <code>ExecutionThread</code> this thread is referring to for variable state
	 *         resolution
	 */
	public final ExecutionThread executionThread() {
		return executionThread;
	}

	@Override
	public Interpreter interpreter() {
		return executionThread.interpreter();
	}

	public static JolieExecutorThread currentThread() {
		final Thread t = Thread.currentThread();
		return (t instanceof JolieExecutorThread) ? (JolieExecutorThread) t : null;
	}
}
