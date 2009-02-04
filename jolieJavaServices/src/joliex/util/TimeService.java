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

package joliex.util;


import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import jolie.net.CommMessage;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class TimeService extends JavaService
{
	protected class TimeThread extends Thread
	{
		final private long waitTime;
		final private String callbackOperation;
		final private Value callbackValue;
		final private TimeService parent;
		public TimeThread( TimeService parent, long waitTime, String callbackOperation, Value callbackValue )
		{
			this.waitTime = waitTime;
			this.callbackOperation =
					( callbackOperation == null ) ? "timeout" : callbackOperation;
			this.callbackValue =
					( callbackValue == null ) ? Value.create() : callbackValue;
			this.parent = parent;
		}
		
		@Override
		public void run()
		{
			try {
				Thread.sleep( waitTime );
				parent.sendMessage( new CommMessage( callbackOperation, "/", callbackValue ) );
			} catch( InterruptedException e ) {}
		}
	}
	
	private TimeThread thread = null;
	final private DateFormat dateFormat, dateTimeFormat;
	
	public TimeService()
	{
		dateFormat = DateFormat.getDateInstance( DateFormat.SHORT );
		dateTimeFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM );
	}
	
	@Override
	protected void finalize()
	{
		if ( thread != null ) {
			thread.interrupt();
		}
	}
	
	private void launchTimeThread( long waitTime, String callbackOperation, Value callbackValue )
	{
		waitTime = ( waitTime > 0 ) ? waitTime : 0L;
		if ( thread != null ) {
			thread.interrupt();
		}
		thread = new TimeThread( this, waitTime, callbackOperation, callbackValue );
		thread.start();
	}
	
	public void setNextTimeout( CommMessage message )
	{
		long waitTime = message.value().intValue();
		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if ( (vec=message.value().children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		if ( (vec=message.value().children().get( "message" )) != null ) {
			callbackValue = vec.first();
		}
		
		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}
	
	public void setNextTimeoutByDateTime( CommMessage message )
	{
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				Date date = dateTimeFormat.parse( message.value().strValue() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException e ) {}

		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if ( (vec=message.value().children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		if ( (vec=message.value().children().get( "message" )) != null ) {
			callbackValue = vec.first();
		}
		
		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}
	
	public void setNextTimeoutByTime( CommMessage message )
	{
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				String today = dateFormat.format( new Date() );
				Date date = dateTimeFormat.parse( today + " " + message.value().strValue() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException pe ) {}

		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if ( (vec=message.value().children().get( "operation" )) != null )
			callbackOperation = vec.first().strValue();
		if ( (vec=message.value().children().get( "message" )) != null )
			callbackValue = vec.first();
		
		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}
	
	public CommMessage sleep( CommMessage message )
	{
		int millis = message.value().intValue();
		try {
			if ( millis > 0 ) {
				Thread.sleep( millis );
			}
		} catch ( InterruptedException e ) {
		}

		return CommMessage.createResponse( message, Value.create() );
	}
	
	public CommMessage currentTimeMillis( CommMessage message )
	{
		return CommMessage.createResponse( message, Value.create( (int)System.currentTimeMillis() ) );
	}
	
	public CommMessage getCurrentDateTime( CommMessage message )
	{
		return CommMessage.createResponse( message, Value.create( dateTimeFormat.format( new Date() ) ) );
	}
}
