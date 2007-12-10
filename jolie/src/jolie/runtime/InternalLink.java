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

import java.util.LinkedList;
import java.util.NoSuchElementException;

import jolie.ExecutionThread;
import jolie.net.CommMessage;
import jolie.process.InputProcess;
import jolie.util.Pair;


/** Internal synchronization link for parallel processes.
 * 
 * @author Fabrizio Montesi
 * 
 * @todo improve efficiency of linkIn list
 */
public class InternalLink implements InputHandler
{
	//private LinkedList< Thread > inList = new LinkedList< Thread >();
	private LinkedList< Pair< ExecutionThread, InputProcess > > inList = new LinkedList< Pair< ExecutionThread, InputProcess > >();
	//private HashMap< Thread, InputProcess > inMap = new HashMap< Thread, InputProcess >();
	private LinkedList< Thread > outList = new LinkedList< Thread >();
	
	/*private LinkedList< InputProcess > procsList;
	private LinkedList< Process > outList;*/
	
	private String id;
	
	public InternalLink( String id )
	{
		this.id = id;
	}
	
	public String id()
	{
		return id;
	}
	
	public synchronized void signForMessage( InputProcess process )
	{
		synchronized( Thread.currentThread() ) {
			if ( outList.isEmpty() )
				inList.addFirst(
					new Pair< ExecutionThread, InputProcess >( ExecutionThread.currentThread(), process ) );
				//inMap.put( Thread.currentThread(), process );
				//inMa.addFirst( process );
			else {
				process.recvMessage( null, new CommMessage( id() ) );
				Thread t = outList.removeLast();
				synchronized( t ) {
					t.notify();
				}
			}
		}
	}
	
	public synchronized void cancelWaiting( InputProcess process ) 
	{
		for( Pair< ExecutionThread, InputProcess > pair : inList ) {
			if ( pair.key() == ExecutionThread.currentThread() ) {
				inList.remove( pair );
				break;
			}
		}
		//inMap.remove( Thread.currentThread() );
	}

	public void linkIn( InputProcess process )
	{
		Pair< ExecutionThread, InputProcess > pair =
			new Pair< ExecutionThread, InputProcess >( ExecutionThread.currentThread(), process );

		Thread t = null, currThread = Thread.currentThread();
		synchronized( this ) {
			try {
				t = outList.removeLast();
			} catch( NoSuchElementException e ) {
				inList.addFirst( pair );
				//inMap.put( currThread, process );
				//inList.addFirst( currThread );
			} 
		}
		
		synchronized( currThread ) {
			if ( t == null ) {
				boolean wait = false;
				synchronized( this ) {
					wait = inList.contains( pair );
					//wait = inMap.containsKey( currThread );
					//wait = inList.contains( currThread );
				}
				if ( wait ) {
					try {
						currThread.wait();
					} catch( InterruptedException e ) {}
				}
			} else {
				synchronized( t ) {
					t.notify();
				}
			}
		}
	}
	
	public void linkOut()
	{
		Pair< ExecutionThread, InputProcess > pair = null;
		ExecutionThread currThread = ExecutionThread.currentThread();
		synchronized( this ) {
			try {
				pair = inList.removeLast();
			} catch( NoSuchElementException e ) {
				outList.addFirst( currThread );
			} 
		}
		
		synchronized( currThread ) {
			if ( pair == null ) {
				boolean wait = false;
				synchronized( this ) {
					wait = outList.contains( currThread );
				}
				if ( wait ) {
					try {
						currThread.wait();
					} catch( InterruptedException e ) {}
				}
			} else {
				pair.value().recvMessage( null, new CommMessage( id() ) );
				synchronized( pair.key() ) {
					pair.key().notify();
				}
			}
		}
	}
	
	public static InternalLink getById( String id )
	{
		return ExecutionThread.currentThread().state().getLink( id ); 
	}
}
