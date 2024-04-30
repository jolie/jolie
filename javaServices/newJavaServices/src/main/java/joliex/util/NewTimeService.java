package joliex.util;

import jolie.runtime.JavaService;
import jolie.runtime.Value;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import jolie.Interpreter;
import jolie.InterpreterThread;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.RequestResponse;
import jolie.runtime.embedding.java.JolieValue;
import interfaces.*;
import faults.*;
import types.*;

public final class NewTimeService extends JavaService implements TimeInterface {

	protected static class TimeThread extends Thread implements InterpreterThread {
		private final long waitTime;
		private final String callbackOperation;
		private final Value callbackValue;
		private final NewTimeService parent;

		public TimeThread( NewTimeService parent, long waitTime, String callbackOperation,
			JolieValue callbackValue ) {
			this.waitTime = waitTime;
			this.callbackOperation =
				(callbackOperation == null) ? "timeout" : callbackOperation;
			this.callbackValue =
				(callbackValue == null) ? Value.create() : JolieValue.toValue( callbackValue );
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
	private final DateFormat dateFormat, dateTimeFormat;
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final Map< Long, ScheduledFuture< ? > > scheduledFutureHashMap = new ConcurrentHashMap<>();
	private final AtomicLong atomicLong = new AtomicLong();

	public NewTimeService() {
		dateFormat = DateFormat.getDateInstance( DateFormat.SHORT );
		dateTimeFormat = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.MEDIUM );
	}

	@Override
	protected void finalize()
		throws Throwable {
		try {
			if( thread != null ) {
				thread.interrupt();
			}
		} finally {
			super.finalize();
		}
	}

	private void launchTimeThread( long waitTime, String callbackOperation, JolieValue callbackValue ) {
		waitTime = (waitTime > 0) ? waitTime : 0L;
		if( thread != null ) {
			thread.interrupt();
		}
		thread = new TimeThread( this, waitTime, callbackOperation, callbackValue );
		thread.start();
	}

	public void stopNextTimeout() {
		if( thread != null ) {
			thread.interrupt();
		}
	}

	public void setNextTimeout( SetNextTimeOutRequest request ) {
		launchTimeThread(
			request.content().value(),
			request.operation().orElse( null ),
			request.message().orElse( null ) );
	}

	public void setNextTimeoutByDateTime( JolieValue request ) {
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				Date date = dateTimeFormat.parse( request.content().toString() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException e ) {
		}

		launchTimeThread(
			waitTime,
			request.getFirstChild( "operation" ).map( s -> s.content().toString() ).orElse( null ),
			request.getFirstChild( "message" ).orElse( null ) 
		);
	}

	public void setNextTimeoutByTime( JolieValue request ) {
		long waitTime = 0;
		try {
			synchronized( dateTimeFormat ) {
				String today = dateFormat.format( new Date() );
				Date date = dateTimeFormat.parse( today + " " + request.content().toString() );
				waitTime = date.getTime() - (new Date()).getTime();
			}
		} catch( ParseException pe ) {
		}

		launchTimeThread(
			waitTime,
			request.getFirstChild( "operation" ).map( s -> s.content().toString() ).orElse( null ),
			request.getFirstChild( "message" ).orElse( null ) 
		);
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

	public String getCurrentDateTime( CurrentDateTimeRequestType request ) {
		String result = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat( request.format().orElse( "dd/MM/yyyy HH:mm:ss" ) );
			final Date now = new Date();
			result = sdf.format( now );
		} catch( Exception e ) {
			e.printStackTrace(); // TODO FaultException
		}
		return result;
	}


	public GetDateTimeResponse getDateTime( GetDateTimeRequest request ) {
		try {
			String format = request.format().orElse( "dd/MM/yyyy HH:mm:ss" );
			long tm = request.content().value();
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date timestamp = new Date( tm );
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTimeInMillis( timestamp.getTime() );
			return GetDateTimeResponse.builder()
				.contentValue( sdf.format( timestamp ) )
				.day( cal.get( Calendar.DAY_OF_MONTH ) )
				.month( cal.get( Calendar.MONTH ) + 1 )
				.year( cal.get( Calendar.YEAR ) )
				.hour( cal.get( Calendar.HOUR_OF_DAY ) )
				.minute( cal.get( Calendar.MINUTE ) )
				.second( cal.get( Calendar.SECOND ) )
				.build();
		} catch( Exception e ) {
			e.printStackTrace(); // TODO FaultException
		}
		return null;
	}

	/**
	 * @author Claudio Guidi
	 */
	public DateValuesType getCurrentDateValues() {
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis( new Date().getTime() );
		return DateValuesType.builder()
			.day( cal.get( Calendar.DAY_OF_MONTH ) )
			.month( cal.get( Calendar.MONTH ) + 1 )
			.year( cal.get( Calendar.YEAR ) )
			.build();
	}

	/**
	 * @author Claudio Guidi
	 * @param request
	 * @return
	 */
	public DateValuesType getDateValues( DateValuesRequestType request )
		throws InvalidDate {
		try {
			String format = request.format().orElse( "dd/MM/yyyy" );
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			GregorianCalendar cal = new GregorianCalendar();
			final Date dt = sdf.parse( request.content().value() );
			cal.setTimeInMillis( dt.getTime() );
			return DateValuesType.builder()
				.day( cal.get( Calendar.DAY_OF_MONTH ) )
				.month( cal.get( Calendar.MONTH ) + 1 )
				.year( cal.get( Calendar.YEAR ) )
				.build();
		} catch( ParseException pe ) {
			throw new InvalidDate( JolieValue.of( pe.getMessage() ) );
		}
	}

	/**
	 * @author Balint Maschio
	 * @param request
	 * @return
	 */
	public DateTimeType getDateTimeValues( GetTimestampFromStringRequest request )
		throws InvalidDate {
		try {
			String format = request.format().orElse( "dd/MM/yyyy HH:ss:mm" );
			SimpleDateFormat sdf = request.language()
				.map( language -> {
					if( language.equals( "us" ) )
						return new SimpleDateFormat( format, Locale.ENGLISH );

					if( language.equals( "it" ) )
						return new SimpleDateFormat( format, Locale.ITALIAN );

					return null;
				} )
				.orElse( new SimpleDateFormat( format, Locale.US ) );
			GregorianCalendar cal = new GregorianCalendar();
			final Date dt = sdf.parse( request.content().value() );
			cal.setTimeInMillis( dt.getTime() );
			return DateTimeType.builder()
				.day( cal.get( Calendar.DAY_OF_MONTH ) )
				.month( cal.get( Calendar.MONTH ) + 1 )
				.year( cal.get( Calendar.YEAR ) )
				.hour( cal.get( Calendar.HOUR_OF_DAY ) )
				.minute( cal.get( Calendar.MINUTE ) )
				.second( cal.get( Calendar.SECOND ) )
				.build();
		} catch( ParseException pe ) {
			throw new InvalidDate( JolieValue.of( pe.getMessage() ) );
		}
	}

	/**
	 * @author Balint Maschio 10/2011 - Fabrizio Montesi: convert to using IllegalArgumentException
	 *         instead of regular expressions.
	 */
	public TimeValuesType getTimeValues( String request )
		throws FaultException {
		try {
			DateFormat sdf = new SimpleDateFormat( "kk:mm:ss" );
			Date date = sdf.parse( request );
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis( date.getTime() );
			return TimeValuesType.builder()
				.hour( calendar.get( Calendar.HOUR_OF_DAY ) )
				.minute( calendar.get( Calendar.MINUTE ) )
				.second( calendar.get( Calendar.SECOND ) )
				.build();
		} catch( ParseException e ) {
			throw new FaultException( "InvalidTime", e );
		}
	}

	/**
	 * @author Claudio Guidi 10/2010 - Fabrizio Montesi: some optimizations.
	 */
	public Integer getDateDiff( DiffDateRequestType request )
		throws FaultException {
		try {
			String format = request.format().orElse( "dd/MM/yyyy" );
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date dt1 = sdf.parse( request.date1() );
			final Date dt2 = sdf.parse( request.date2() );
			return (int) ((dt1.getTime() - dt2.getTime()) / (1000 * 60 * 60 * 24));
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}
	}

	public Integer getTimeDiff( GetTimeDiffRequest request )
		throws FaultException {
		try {

			DateFormat sdf = new SimpleDateFormat( "kk:mm:ss" );
			final Date dt1 = sdf.parse( request.time1() );
			final Date dt2 = sdf.parse( request.time2() );

			return (int) (dt1.getTime() - dt2.getTime());
		} catch( ParseException pe ) {
			throw new FaultException( "InvalidDate", pe );
		}
	}

	public TimeValuesType getTimeFromMilliSeconds( Integer request )
		throws FaultException {
		TimeZone timeZone = TimeZone.getTimeZone( "GMT" );
		Calendar calendar = Calendar.getInstance( timeZone );
		calendar.setTimeInMillis( request );
		return TimeValuesType.builder()
			.hour( calendar.get( Calendar.HOUR_OF_DAY ) )
			.minute( calendar.get( Calendar.MINUTE ) )
			.second( calendar.get( Calendar.SECOND ) )
			.build();
	}

	public Long getTimestampFromString( GetTimestampFromStringRequest request )
		throws InvalidTimestamp {
		try {
			String format = request.format().orElse( "dd/MM/yyyy kk:mm:ss" );
			SimpleDateFormat sdf = new SimpleDateFormat( format );
			final Date dt = sdf.parse( request.content().value() );
			return dt.getTime();
		} catch( ParseException pe ) {
			throw new InvalidTimestamp( JolieValue.of( pe.getMessage() ) );
		}
	}

	public Long scheduleTimeout( ScheduleTimeOutRequest request )
		throws InvalidTimeUnit {
		final long timeoutId = atomicLong.getAndIncrement();
		TimeUnit unit;
		try {
			unit = request.timeunit()
				.map( timeunit -> TimeUnit.valueOf( timeunit.toUpperCase() ) )
				.orElse( TimeUnit.MILLISECONDS );

		} catch( Exception e ) {
			throw new InvalidTimeUnit( JolieValue.of( e.getMessage() ) );
		}

		ScheduledFuture< ? > scheduledFuture = executor.schedule( () -> sendMessage( CommMessage.createRequest(
			request.operation().orElse( "timeout" ),
			"/",
			request.message().map( JolieValue::toValue ).orElse( Value.create() ) ) ),
			request.content().value(),
			unit );
		scheduledFutureHashMap.put( timeoutId, scheduledFuture );
		return timeoutId;
	}

	public Boolean cancelTimeout( Long request ) {
		ScheduledFuture< ? > f = scheduledFutureHashMap.remove( request );
		return f != null && f.cancel( false );
	}
}
