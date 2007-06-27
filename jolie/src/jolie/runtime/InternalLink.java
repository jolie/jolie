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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import jolie.CorrelatedThread;
import jolie.net.CommMessage;
import jolie.process.InputProcess;
import jolie.process.NDChoiceProcess;
import jolie.util.Pair;


/** Internal synchronization link for parallel processes.
 * 
 * @author Fabrizio Montesi
 * 
 * @todo improve efficiency of linkIn list
 */
public class InternalLink extends AbstractMappedGlobalObject implements InputHandler
{
	private static HashMap< String, InternalLink > idMap = 
		new HashMap< String, InternalLink >();

	//private LinkedList< Thread > inList = new LinkedList< Thread >();
	private LinkedList< Pair< CorrelatedThread, InputProcess > > inList = new LinkedList< Pair< CorrelatedThread, InputProcess > >();
	//private HashMap< Thread, InputProcess > inMap = new HashMap< Thread, InputProcess >();
	private LinkedList< Thread > outList = new LinkedList< Thread >();
	
	/*private LinkedList< InputProcess > procsList;
	private LinkedList< Process > outList;*/
	
	public InternalLink( String id )
	{
		super( id );
	}
	
	
	
	public synchronized void signForMessage( NDChoiceProcess process )
	{
		synchronized( Thread.currentThread() ) {
			if ( outList.isEmpty() )
				inList.addFirst(
					new Pair< CorrelatedThread, InputProcess >( CorrelatedThread.currentThread(), process ) );
				//inMap.put( Thread.currentThread(), process );
				//inMa.addFirst( process );
			else {
				process.recvMessage( new CommMessage( id() ) );
				Thread t = outList.removeLast();
				synchronized( t ) {
					t.notify();
				}
			}
		}
	}
	
	public synchronized void cancelWaiting( NDChoiceProcess process ) 
	{
		for( Pair< CorrelatedThread, InputProcess > pair : inList ) {
			if ( pair.key() == CorrelatedThread.currentThread() ) {
				inList.remove( pair );
				break;
			}
		}
		//inMap.remove( Thread.currentThread() );
	}

	public void linkIn( InputProcess process )
	{
		Pair< CorrelatedThread, InputProcess > pair =
			new Pair< CorrelatedThread, InputProcess >( CorrelatedThread.currentThread(), process );

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
		Pair< CorrelatedThread, InputProcess > pair = null;
		CorrelatedThread currThread = CorrelatedThread.currentThread();
		synchronized( this ) {
			try {
				pair = inList.removeLast();
				//t =
				//t = inList.removeLast();
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
				CorrelatedThread.setCurrent( pair.key() );
				pair.value().recvMessage( new CommMessage( id() ) );
				CorrelatedThread.setCurrent( null );
				synchronized( pair.key() ) {
					pair.key().notify();
				}
			}
		}
	}
	
	public static InternalLink getById( String id )
		throws InvalidIdException
	{
		InternalLink retVal = idMap.get( id );
		if ( retVal == null )
			throw new InvalidIdException( id );

		return retVal;
	}
	
	public final void register()
	{
		idMap.put( id(), this );
	}
	
	public static Collection< InternalLink > getAll()
	{
		return idMap.values();
	}
}
