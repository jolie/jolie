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


import java.lang.ref.Cleaner;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import jolie.Interpreter;
import jolie.InterpreterThread;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

public class TimeService extends JavaService {
	protected static class TimeThread extends Thread implements InterpreterThread {
		private final long waitTime;
		private final String callbackOperation;
		private final Value callbackValue;
		private final TimeService parent;

		public TimeThread( TimeService parent, long waitTime, String callbackOperation, Value callbackValue ) {
			this.waitTime = waitTime;
			this.callbackOperation =
				(callbackOperation == null) ? "timeout" : callbackOperation;
			this.callbackValue =
				(callbackValue == null) ? Value.create() : callbackValue;
			this.parent = parent;
		}

		@Override
		public void run() {
			try {
				Thread.sleep( waitTime );
				parent.sendMessage( CommMessage.createRequest( callbackOperation, "/", callbackValue ) );
			} catch( InterruptedException e ) {
			}
		}

		@Override
		public Interpreter interpreter() {
			return parent.interpreter();
		}
	}

	private TimeThread thread = null;
	private final Cleaner cleaner = Cleaner.create();

	private final DateFormat dateFormat, dateTimeFormat;
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final Map< Long, ScheduledFuture< ? > > scheduledFutureHashMap = new ConcurrentHashMap<>();
	private final AtomicLong atomicLong = new AtomicLong();

	public TimeService() {
		dateFormat = DateFormat.getDateInstance( DateFormat.SHORT );
		dateTimeFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM );
	}

	private void launchTimeThread( long waitTime, String callbackOperation, Value callbackValue ) {
		waitTime = (waitTime > 0) ? waitTime : 0L;
		if( thread != null ) {
			thread.interrupt();
		}
		thread = new TimeThread( this, waitTime, callbackOperation, callbackValue );
		cleaner.register( thread, thread::interrupt );
		thread.start();
	}

	public void stopNextTimeout( Value request ) {
		if( thread != null ) {
			thread.interrupt();
		}
	}

	public void setNextTimeout( Value request ) {
		long waitTime = request.intValue();
		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if( (vec = request.children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		if( (vec = request.children().get( "message" )) != null ) {
			callbackValue = vec.first();
		}

		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}

	public void setNextTimeoutByDateTime( Value request ) {
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				Date date = dateTimeFormat.parse( request.strValue() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException e ) {
		}

		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if( (vec = request.children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		if( (vec = request.children().get( "message" )) != null ) {
			callbackValue = vec.first();
		}

		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}

	public void setNextTimeoutByTime( Value request ) {
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				String today = dateFormat.format( new Date() );
				Date date = dateTimeFormat.parse( today + " " + request.strValue() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException pe ) {
		}

		String callbackOperation = null;
		ValueVector vec;
		Value callbackValue = null;
		if( (vec = request.children().get( "operation" )) != null )
			callbackOperation = vec.first().strValue();
		if( (vec = request.children().get( "message" )) != null )
			callbackValue = vec.first();

		launchTimeThread( waitTime, callbackOperation, callbackValue );
	}

	@RequestResponse
	public void sleep( Integer millis ) {
		try {
			if( millis > 0 ) {
				Thread.sleep( millis );
			}
		} catch( InterruptedException e ) {
		}
	}

	public Long getCurrentTimeMillis() {
		return System.currentTimeMillis();
	}

	public String getCurrentDateTime( Value request ) {
		String result = null;
		try {
			String format;
			if( request.getFirstChild( "format" ).strValue().isEmpty() ) {
				format = "dd/MM/yyyy HH:mm:ss";
			} else {
				format = request.getFirstChild( "format" ).strValue();
			}
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date now = new Date();
			result = sdf.format( now );
		} catch( Exception e ) {
			e.printStackTrace(); // TODO FaultException
		}
		return result;
	}


	public Value getDateTime( Value request ) {
		Value result = Value.create();
		try {
			String format;
			if( request.getFirstChild( "format" ).strValue().isEmpty() ) {
				format = "dd/MM/yyyy HH:mm:ss";
			} else {
				format = request.getFirstChild( "format" ).strValue();
			}
			long tm = request.longValue();
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date timestamp = new Date( tm );
			result.setValue( sdf.format( timestamp ) );
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( timestamp.getTime() );
			result.getFirstChild( "day" ).setValue( cal.get( Calendar.DAY_OF_MONTH ) );
			result.getFirstChild( "month" ).setValue( cal.get( Calendar.MONTH ) + 1 );
			result.getFirstChild( "year" ).setValue( cal.get( Calendar.YEAR ) );
			result.getFirstChild( "hour" ).setValue( cal.get( Calendar.HOUR_OF_DAY ) );
			result.getFirstChild( "minute" ).setValue( cal.get( Calendar.MINUTE ) );
			result.getFirstChild( "second" ).setValue( cal.get( Calendar.SECOND ) );
		} catch( Exception e ) {
			e.printStackTrace(); // TODO FaultException
		}
		return result;
	}

	/**
	 * @author Claudio Guidi
	 */
	public Value getCurrentDateValues() {
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
	 * @param request
	 * @return
	 * @throws jolie.runtime.FaultException
	 */
	public Value getDateValues( Value request )
		throws FaultException {
		Value v = Value.create();
		try {
			String format;
			if( request.getFirstChild( "format" ).strValue().isEmpty() ) {
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
	 * @author Balint Maschio
	 * @param request
	 * @return
	 * @throws jolie.runtime.FaultException
	 */
	public Value getDateTimeValues( Value request )
		throws FaultException {
		SimpleDateFormat sdf;
		Value v = Value.create();
		try {
			String format;
			if( request.getFirstChild( "format" ).strValue().isEmpty() ) {
				format = "dd/MM/yyyy HH:ss:mm";
			} else {
				format = request.getFirstChild( "format" ).strValue();
			}
			if( request.hasChildren( "language" ) ) {
				String language = request.getFirstChild( "language" ).strValue();
				if( language.equals( "us" ) ) {
					sdf = new SimpleDateFormat( format, Locale.ENGLISH );
				} else if( language.equals( "it" ) ) {
					sdf = new SimpleDateFormat( format, Locale.ITALIAN );
				} else {
					sdf = new SimpleDateFormat( format, Locale.US );
				}
			} else {
				sdf = new SimpleDateFormat( format, Locale.US );
			}

			GregorianCalendar cal = new GregorianCalendar();
			final Date dt = sdf.parse( request.strValue() );
			cal.setTimeInMillis( dt.getTime() );
			v.getFirstChild( "day" ).setValue( cal.get( Calendar.DAY_OF_MONTH ) );
			v.getFirstChild( "month" ).setValue( cal.get( Calendar.MONTH ) + 1 );
			v.getFirstChild( "year" ).setValue( cal.get( Calendar.YEAR ) );
			v.getFirstChild( "hour" ).setValue( cal.get( Calendar.HOUR_OF_DAY ) );
			v.getFirstChild( "minute" ).setValue( cal.get( Calendar.MINUTE ) );
			v.getFirstChild( "second" ).setValue( cal.get( Calendar.SECOND ) );

		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}

		return v;
	}

	/**
	 * @author Balint Maschio 10/2011 - Fabrizio Montesi: convert to using IllegalArgumentException
	 *         instead of regular expressions.
	 */
	public Value getTimeValues( Value request )
		throws FaultException {
		try {
			Value v = Value.create();
			DateFormat sdf = new SimpleDateFormat( "kk:mm:ss" );
			Date date = sdf.parse( request.strValue() );
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis( date.getTime() );
			v.getFirstChild( "hour" ).setValue( calendar.get( Calendar.HOUR_OF_DAY ) );
			v.getFirstChild( "minute" ).setValue( calendar.get( Calendar.MINUTE ) );
			v.getFirstChild( "second" ).setValue( calendar.get( Calendar.SECOND ) );
			return v;
		} catch( ParseException e ) {
			throw new FaultException( "InvalidTime", e );
		}
	}

	/**
	 * @author Claudio Guidi 10/2010 - Fabrizio Montesi: some optimizations.
	 */
	public Value getDateDiff( Value request )
		throws FaultException {
		Value v = Value.create();
		try {
			String format;
			if( request.hasChildren( "format" ) ) {
				format = request.getFirstChild( "format" ).strValue();
			} else {
				format = "dd/MM/yyyy";
			}
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date dt1 = sdf.parse( request.getFirstChild( "date1" ).strValue() );
			final Date dt2 = sdf.parse( request.getFirstChild( "date2" ).strValue() );
			v.setValue( (int) ((dt1.getTime() - dt2.getTime()) / (1000 * 60 * 60 * 24)) );
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}
		return v;
	}

	public Value getTimeDiff( Value request )
		throws FaultException {
		Value v = Value.create();
		try {

			DateFormat sdf = new SimpleDateFormat( "kk:mm:ss" );
			final Date dt1 = sdf.parse( request.getFirstChild( "time1" ).strValue() );
			final Date dt2 = sdf.parse( request.getFirstChild( "time2" ).strValue() );

			v.setValue( dt1.getTime() - dt2.getTime() );
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}
		return v;
	}

	public Value getTimeFromMilliSeconds( Value request )
		throws FaultException {
		Value v = Value.create();
		TimeZone timeZone = TimeZone.getTimeZone( "GMT" );

		Calendar calendar = Calendar.getInstance( timeZone );
		calendar.setTimeInMillis( request.longValue() );
		v.getFirstChild( "hour" ).setValue( calendar.get( Calendar.HOUR_OF_DAY ) );
		v.getFirstChild( "minute" ).setValue( calendar.get( Calendar.MINUTE ) );
		v.getFirstChild( "second" ).setValue( calendar.get( Calendar.SECOND ) );
		return v;
	}

	public Long getTimestampFromString( Value request )
		throws FaultException {
		try {
			String format;
			if( request.getFirstChild( "format" ).strValue().isEmpty() ) {
				format = "dd/MM/yyyy kk:mm:ss";
			} else {
				format = request.getFirstChild( "format" ).strValue();
			}
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date dt = sdf.parse( request.strValue() );

			return dt.getTime();
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidTimestamp", pe );
		}
	}

	@RequestResponse
	public Long scheduleTimeout( Value request )
		throws FaultException {
		final long timeoutId = atomicLong.getAndIncrement();
		TimeUnit unit;

		if( request.hasChildren( "timeunit" ) ) {
			try {
				unit = TimeUnit.valueOf( request.getFirstChild( "timeunit" ).strValue().toUpperCase() );
			} catch( Exception e ) {
				throw new FaultException( "InvalidTimeUnit", e );
			}
		} else {
			unit = TimeUnit.MILLISECONDS;
		}

		String operationName;
		if( request.hasChildren( "operation" ) ) {
			operationName = request.getFirstChild( "operation" ).strValue();
		} else {
			operationName = "timeout";
		}

		ScheduledFuture< ? > scheduledFuture = executor.schedule( () -> {
			sendMessage( CommMessage.createRequest( operationName, "/", request.getFirstChild( "message" ) ) );
		}, request.intValue(), unit );
		scheduledFutureHashMap.put( timeoutId, scheduledFuture );
		return timeoutId;
	}

	@RequestResponse
	public Boolean cancelTimeout( Value request ) {
		long timeoutId = request.longValue();
		ScheduledFuture< ? > f = scheduledFutureHashMap.remove( timeoutId );
		return f != null && f.cancel( false );
	}
}
