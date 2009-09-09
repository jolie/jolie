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

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.JolieThread;

/**
 * <code>CommChannelHandler</code> is a <code>JolieThread</code> used by
 * <code>CommCore</code> to handle incoming communications.
 * @see JolieThread
 * @see CommCore
 * @author fmontesi
 */
public class CommChannelHandler extends JolieThread
{
	private ExecutionThread executionThread;
	
	protected CommChannelHandler( Interpreter interpreter )
	{
		super( interpreter );
	}

	/**
	 * Constructor
	 * @param interpreter the interpreter in whose scope this <code>CommChannelHandler</code> will be used
	 * @param r the <code>Runnable</code> to execute in this thread
	 * @see Interpreter
	 * @see Runnable
	 */
	public CommChannelHandler( Interpreter interpreter, Runnable r )
	{
		super( interpreter, r );
	}

	/**
	 * Returns the current <code>CommChannelHandler</code> thread.
	 * This method must be called only if the caller is sure that the current
	 * thread is a <code>CommChannelHandler</code>.
	 * @return the current <code>CommChannelHandler</code> thread
	 */
	public static CommChannelHandler currentThread()
	{
		return ((CommChannelHandler)Thread.currentThread());
	}

	/**
	 * Sets the <code>ExecutionThread</code> this thread must refer to.
	 * This is needed to refer to the right variable state when in this thread.
	 * @param thread the <code>ExecutionThread</code> this thread must refer to for variable state resolution
	 */
	public void setExecutionThread( ExecutionThread thread )
	{
		executionThread = thread;
	}

	/**
	 * Returns the <code>ExecutionThread</code> this thread is referring to for variable state resolution.
	 * @return the <code>ExecutionThread</code> this thread is referring to for variable state resolution
	 */
	public ExecutionThread executionThread()
	{
		return executionThread;
	}
}
