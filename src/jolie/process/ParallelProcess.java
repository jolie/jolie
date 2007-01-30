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

import java.util.Vector;

import org.w3c.dom.Node;

class ParallelThread extends Thread
{
	Process process;
	ParallelProcess parent;
	
	public ParallelThread( Process process, ParallelProcess parent )
	{
		this.process = process;
		this.parent = parent;
	}
	
	public void run()
	{
		process.run();
		parent.terminationNotify();
	}
}

public class ParallelProcess implements Process, Optimizable
{
	private Vector< Process > children;
	private int runningProcesses;
	
	public ParallelProcess()
	{
		children = new Vector< Process >();
		runningProcesses = 0;
	}
	
	public void run()
	{
		runningProcesses = children.size();
		if ( runningProcesses > 1 ) {
			for( Process proc : children )
				new ParallelThread( proc, this ).start();

			try {
				//while( runningProcesses > 0 )
				synchronized( this ) {
					wait();
				}
			} catch( InterruptedException e ) {}
		} else if ( runningProcesses == 1 ) // No multi-threading required!
			children.elementAt( 0 ).run();

	}
	
	public synchronized void terminationNotify()
	{
		runningProcesses--;
		if ( runningProcesses < 1 )
			notify();
	}
	
	public void addChild( Process process )
	{
		if ( process != null )
			children.add( process );
	}
	
	public Process optimize()
	{
		if ( children.size() == 1 ) {
			if ( children.elementAt( 0 ) instanceof Optimizable )
				return ((Optimizable)children.elementAt( 0 )).optimize();
			else
				return children.elementAt( 0 );
		}
				
		Process proc;
		for( int i = 0; i < children.size(); i++ ) {
			proc = children.elementAt( i );
			if ( proc instanceof Optimizable )
				children.set( i, ((Optimizable)proc).optimize() );
		}
		return this;
	}
	
	public void translateToBPEL( Node parentNode )
	{
		
	}
}
