/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

import java.util.LinkedList;

import jolie.net.CommMessage;
import jolie.process.InputProcess;


abstract public class InputOperation extends Operation implements InputHandler
{
	private LinkedList< InputProcess > procsList;
	private LinkedList< CommMessage > mesgList;
	
	public InputOperation( String id )
	{
		super( id );
		procsList = new LinkedList< InputProcess >();
		mesgList = new LinkedList< CommMessage >();
	}
	
	public static InputOperation getById( String id )
		throws InvalidIdException
	{
		Object obj = Interpreter.getObjectById( id );
		if ( !( obj instanceof InputOperation ) )
			throw new InvalidIdException( id );
		return (InputOperation)obj;
	}
	
	public void recvMessage( CommMessage message )
	{
		boolean received = false;
		InputProcess process;
		/*
		 * todo -- deprecate use of received?
		 */
		while ( !received ) {
			process = null;
			synchronized( this ) {
				if ( !procsList.isEmpty() )
					process = procsList.removeLast();
				else {
					mesgList.addFirst( message );
					received = true;
				}
			}
			if ( process != null ) {
				synchronized( process ) {
					received = process.recvMessage( message );
					if ( received )
						process.notify();
				}
			}
		}
	}
	
	public void getMessage( InputProcess process, boolean wait )
	{
		CommMessage message = null;

		synchronized( this ) {
			if ( !mesgList.isEmpty() )
				message = mesgList.removeLast();
			else
				procsList.addFirst( process );
		}
		
		if ( message != null ) {
			if ( !process.recvMessage( message ) ) {
				synchronized( this ) {
					mesgList.addLast( message );
				}
			}
		} else if ( wait ) {
			synchronized( process ) {
				try {
					process.wait();
				} catch( InterruptedException e ) {}
			}
		}
	}
	
	public void getMessage( InputProcess process )
	{
		getMessage( process, true );
	}
		
	public synchronized void signForMessage( InputProcess process )
	{
		if ( mesgList.isEmpty() )
			procsList.addFirst( process );
		else
			process.recvMessage( mesgList.removeLast() );
	}
	
	public synchronized void cancelWaiting( InputProcess process ) 
	{
		procsList.remove( process );
	}
}