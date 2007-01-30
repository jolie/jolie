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

import jolie.Expression;
import jolie.InputHandler;
import jolie.net.CommMessage;

import org.w3c.dom.Node;

class SleepInputHandler extends Thread implements InputHandler
{
	private InputProcess inputProcess;
	private Expression expression;
	
	//private TypedValue< int > value;
	
	public SleepInputHandler( Expression expression )
	{
		this.expression = expression;
	}
	
	public void run()
	{
		int i = expression.evaluate().intValue();
		try {
			if ( i > 0 )
				Thread.sleep( i );
			if ( inputProcess != null )
				inputProcess.recvMessage( new CommMessage( id() ) );
		} catch( InterruptedException e ) {}
	}
	
	public String id()
	{
		return this.toString();
	}
	
	public synchronized void signForMessage( InputProcess process )
	{
		inputProcess = process;
		if ( this.getState() == Thread.State.NEW )
			this.start();
	}
	
	public synchronized void cancelWaiting( InputProcess process )
	{
		if ( inputProcess == process )
			inputProcess = null;
		
		this.interrupt();
	}
}

public class SleepProcess implements InputProcess
{
	private Expression expression;
	private SleepInputHandler inputHandler = null;
	
	public SleepProcess( Expression expression )
	{
		this.expression = expression;
	}
	
	public void run()
	{
		int i = expression.evaluate().intValue();
		try {
			if ( i > 0 )
				Thread.sleep( i );
		} catch( InterruptedException e ) {}
	}
	
	public InputHandler inputHandler()
	{
		if ( inputHandler == null )
			inputHandler = new SleepInputHandler( expression );

		return inputHandler;
	}
	
	public boolean recvMessage( CommMessage message )
	{
		return true;
	}
	
	public void translateToBPEL( Node parentNode )
	{
		
	}
}
