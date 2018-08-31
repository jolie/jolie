/***************************************************************************
 *   Copyright (C) 2011 by F. Bullini <fbullini@italianasoftware.com>      *
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

package joliex.scheduler;

import java.util.Date;
import java.util.GregorianCalendar;
import jolie.runtime.AndJarDeps;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.scheduler.impl.JolieSchedulerDefaultJob;
import static org.quartz.CronScheduleBuilder.dailyAtHourAndMinute;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

@AndJarDeps( {"quartz-2.0.0.jar", "slf4j-api-1.6.1.jar", "jta-1.1.jar", "c3p0-0.9.1.1.jar"} ) //  c3po
public class SchedulerService extends JavaService
{
	public void setDailySchedule( Value request )
	{
		System.out.println( "setDailySchedule() has been deprecated and could be removed in future" );

		String callbackOperation = "timeout";
		int start_hour = 21;
		int start_min = 0;
		ValueVector vec;
		if ( (vec = request.children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		/*else { callbackValue = vec.first(); }
		 */
		if ( (vec = request.children().get( "start_hour" )) != null ) {
			start_hour = vec.first().intValue();
		} else {//dovrebbe partire un TypeMismatch jolie
			System.out.println( " start_hour not present:It will be set to currente date" );
		}/*
		if ((vec = request.children().get("start_min")) != null) {
		start_min=vec.first().intValue();
		} else {//dovrebbe partire un TypeMismatch jolie
		System.out.println(" start_min not present:It will be set to currente date");
		}*/
		System.out.println( " start_hour =" + start_hour );
		try {
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.start();
//-----------------------------------------
			// define the job and tie it to our HelloJob class
			JobDetail job = org.quartz.JobBuilder.newJob( JolieSchedulerDefaultJob.class ).withIdentity( "job1", "group1" ).build();
			job.getJobDataMap().put( "operation", callbackOperation );
			job.getJobDataMap().put( "javaSchedulerService", this );
			//job.getJobDataMap().put("operation", callbackOperation);
			//SimpleTrigger trigger = (SimpleTrigger) newTrigger().withDescription("holatriger").withIdentity("trigger_" + count, "group1").startAt(startTime).build();
			// Trigger the job to run now, and then repeat every 40 seconds
			Trigger trigger = org.quartz.TriggerBuilder.newTrigger().withIdentity( "trigger1", "group1" ) //TODO: usare chronSchedule("0 0 10am 1,15 * ?  ")
				//.startAt(startDate)
				.withSchedule( dailyAtHourAndMinute( start_hour, start_min ) ).build();
			// Se viene rischiesto un trigger con lo stesso nome, il precedente viene rimosso
			if ( scheduler.checkExists( trigger.getKey() ) ) {
				System.out.println( " trigger (con stesso NOME) già innescato triggger.getStatTime=" + trigger.getStartTime() );
				System.out.println( "previusly scheduled " + trigger + " has state=" + scheduler.getTriggerState( trigger.getKey() ) );
				scheduler.unscheduleJob( trigger.getKey() );
			}
			
			//scheduler.getTriggerKeys(null)
			scheduler.scheduleJob( job, trigger );
			System.out.println( "just scheduled " + trigger + " has state=" + scheduler.getTriggerState( trigger.getKey() ) );
//------------------------------------------
			//scheduler.shutdown();//TODO chiarire se ci va
		} catch( SchedulerException se ) {
			se.printStackTrace();
			System.out.println( " Exception msg=" + se.getMessage() );
		}
	}

	public void setSchedule( Value request )
	{
		System.out.println( "setSchedule() has been deprecated and could be removed in future" );

		String callbackOperation = "timeout";
		Long period = (long) 60 * 60 * 1000;
		Date startDate = new Date();
		ValueVector vec;
		if ( (vec = request.children().get( "operation" )) != null ) {
			callbackOperation = vec.first().strValue();
		}
		/*else { callbackValue = vec.first(); }
		 */
		if ( (vec = request.children().get( "start_date" )) != null ) {
			System.out.println( " start_date =" + vec.first() );
			GregorianCalendar gc = new GregorianCalendar();
			gc.add( GregorianCalendar.YEAR, vec.get( 1 ).getChildren( "year" ).get( 0 ).intValue() );
			gc.add( GregorianCalendar.MONTH + 1, vec.get( 1 ).getChildren( "month" ).get( 0 ).intValue() );
			gc.add( GregorianCalendar.DAY_OF_MONTH, vec.get( 1 ).getChildren( "day" ).get( 0 ).intValue() );
			gc.add( GregorianCalendar.MINUTE, vec.get( 1 ).getChildren( "min" ).get( 0 ).intValue() );
			gc.add( GregorianCalendar.SECOND, vec.get( 1 ).getChildren( "sec" ).get( 0 ).intValue() );
			gc.add( GregorianCalendar.MILLISECOND, vec.get( 1 ).getChildren( "msec" ).get( 0 ).intValue() );
			startDate = gc.getTime();
		} else {
			System.out.println( " start_date not present:set to current date" );
		}
		System.out.println( " start_date =" + startDate );

		//else { callbackValue = new Date(); }
		/*
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis( new Date().getTime() );
		 */
		if ( (vec = request.children().get( "period" )) != null ) {
			//System.out.println(" vec.get(0)=" + vec.get(0).strValue());
			period = Long.valueOf( vec.get( 0 ).intValue() );
			System.out.println( " period=" + period );
			//TODO:
		} else {
			System.out.println( " period not present:set to 1 hour" );
		}
		//else { callbackValue = vec.first(); }
		try {
			/*
			SchedulerFactory sf = new StdSchedulerFactory();
			Scheduler scheduler = sf.getScheduler();*/
			// Grab the Scheduler instance from the Factory
			Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
			// and start it off
			scheduler.start();
//-----------------------------------------
			// define the job and tie it to our HelloJob class
			JobDetail job = org.quartz.JobBuilder.newJob( JolieSchedulerDefaultJob.class ).withIdentity( "job1", "group1" ).build();
			job.getJobDataMap().put( "operation", callbackOperation );
			job.getJobDataMap().put( "javaSchedulerService", this );
			//job.getJobDataMap().put("operation", callbackOperation);
			//SimpleTrigger trigger = (SimpleTrigger) newTrigger().withDescription("holatriger").withIdentity("trigger_" + count, "group1").startAt(startTime).build();
			// Trigger the job to run now, and then repeat every 40 seconds
			Trigger trigger = org.quartz.TriggerBuilder.newTrigger().withIdentity( "trigger1", "group1" ) //TODO: usare chronSchedule("0 0 10am 1,15 * ?  ")
				.startAt( startDate ).withSchedule( org.quartz.SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds( period ).repeatForever() ).build();
			// Se viene rischiesto un trigger con lo stesso nome, il precedente viene rimosso
			if ( scheduler.checkExists( trigger.getKey() ) ) {
				System.out.println( " trigger (con stesso NOME) già innescato trigger.getStatTime=" + trigger.getStartTime() );
				System.out.println( "previusly scheduled trigger " + trigger + " has state=" + scheduler.getTriggerState( trigger.getKey() ) );
				scheduler.unscheduleJob( trigger.getKey() );
			}
			
			//scheduler.getTriggerKeys(null)
			scheduler.scheduleJob( job, trigger );
			System.out.println( "just scheduled trigger " + trigger + " has state=" + scheduler.getTriggerState( trigger.getKey() ) );
//------------------------------------------
			//scheduler.shutdown();//TODO chiarire se ci va

		} catch( SchedulerException se ) {
			se.printStackTrace();
			System.out.println( " Exception msg=" + se.getMessage() );
		}
	}
}
