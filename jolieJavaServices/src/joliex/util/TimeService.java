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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import java.util.GregorianCalendar;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

public class TimeService extends JavaService
{
	protected class TimeThread extends Thread
	{
		private final long waitTime;
		private final String callbackOperation;
		private final Value callbackValue;
		private final TimeService parent;
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
				parent.sendMessage( CommMessage.createOneWayMessage( callbackOperation, "/", callbackValue ) );
			} catch( InterruptedException e ) {}
		}
	}

	private TimeThread thread = null;
	private final DateFormat dateFormat, dateTimeFormat;

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

	public void setNextTimeout( Value request )
	{
		long waitTime = request.intValue();
		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if ( (vec=request.children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		if ( (vec=request.children().get( "message" )) != null ) {
			callbackValue = vec.first();
		}

		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}

	public void setNextTimeoutByDateTime( Value request )
	{
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				Date date = dateTimeFormat.parse( request.strValue() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException e ) {}

		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if ( (vec=request.children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		if ( (vec=request.children().get( "message" )) != null ) {
			callbackValue = vec.first();
		}

		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}

	public void setNextTimeoutByTime( Value request )
	{
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				String today = dateFormat.format( new Date() );
				Date date = dateTimeFormat.parse( today + " " + request.strValue() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException pe ) {}

		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if ( (vec=request.children().get( "operation" )) != null )
			callbackOperation = vec.first().strValue();
		if ( (vec=request.children().get( "message" )) != null )
			callbackValue = vec.first();

		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}

	@RequestResponse
	public void sleep( Integer millis )
	{
		try {
			if ( millis > 0 ) {
				Thread.sleep( millis );
			}
		} catch ( InterruptedException e ) {
		}
	}

	public Integer currentTimeMillis()
	{
		return (int)System.currentTimeMillis();
	}

	public String getCurrentDateTime()
	{
		return dateTimeFormat.format( new Date() );
	}

	/**
	 * @author Claudio Guidi
	 */
	public Value getCurrentDateValues()
	{
		Value v = Value.create();

		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis( new Date().getTime() );
		v.getFirstChild( "day" ).setValue( cal.get( Calendar.DAY_OF_MONTH ) );
		v.getFirstChild( "month" ).setValue( cal.get( Calendar.MONTH ) + 1 );
		v.getFirstChild( "year" ).setValue( cal.get( Calendar.YEAR ) );

		return v;
	}

	/**
	 * @author Claudio Guidi
	 */
	public Value getDateValues( Value request )
		throws FaultException
	{
		Value v = Value.create();
		try {
			String format;
			if ( request.getFirstChild( "format" ).strValue().isEmpty() ) {
				format = "dd/MM/yyyy";
			} else {
				format = request.getFirstChild( "format" ).strValue();
			}
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			GregorianCalendar cal = new GregorianCalendar();
			final Date dt = sdf.parse( request.strValue() );
			cal.setTimeInMillis( dt.getTime() );
			v.getFirstChild( "day" ).setValue( cal.get( Calendar.DAY_OF_MONTH ) );
			v.getFirstChild( "month" ).setValue( cal.get( Calendar.MONTH ) + 1 );
			v.getFirstChild( "year" ).setValue( cal.get( Calendar.YEAR ) );
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}

		return v;
	}

	/**
	 * @author Claudio Guidi
	 * 10/2010 - Fabrizio Montesi: some optimizations.
	 */
	public Value getDateDiff( Value request )
		throws FaultException
	{
		Value v = Value.create();
		try {
			String format;
			if ( request.hasChildren( "format" ) ) {
				format = request.getFirstChild( "format" ).strValue();
			} else {
				format = "dd/MM/yyyy";
			}
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date dt1 = sdf.parse( request.getFirstChild( "date1" ).strValue() );
			final Date dt2 = sdf.parse( request.getFirstChild( "date2" ).strValue() );
			Long result = new Long( (dt1.getTime() - dt2.getTime()) / (1000 * 60 * 60 * 24) );
			v.setValue( result.intValue() );
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}
		return v;
	}
}
