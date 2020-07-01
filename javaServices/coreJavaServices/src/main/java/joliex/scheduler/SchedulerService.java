/***************************************************************************
 *   Copyright (C) by Francesco Bullini, refactored by Claudio Guidi       *
 *
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;

/**
 *
 * @author claudio guidi
 */
@AndJarDeps( { "quartz-2.2.1.jar", "quartz-jobs-2.2.1.jar", "slf4j-api-1.6.6.jar", "c3p0-0.9.1.1.jar" } )
public class SchedulerService extends JavaService {

	private final Scheduler scheduler;
	private String operationName = "schedulerCallback";

	public SchedulerService() {
		super();

		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();
			scheduler.getContext().put( "schedulerService", this );
			scheduler.start();
		} catch( SchedulerException e ) {
			throw new RuntimeException( e );
		}
	}

	public String getOperationName() {
		return operationName;
	}

	@RequestResponse
	public void setCallbackOperation( Value request ) {
		operationName = request.getFirstChild( "operationName" ).strValue();
	}

	@SuppressWarnings( "PMD" )
	@RequestResponse
	public Value setCronJob( Value request ) throws FaultException {

		String jobName = request.getFirstChild( "jobName" ).strValue();
		String groupName = request.getFirstChild( "groupName" ).strValue();
		String seconds = request.getFirstChild( "cronSpecs" ).getFirstChild( "second" ).strValue();
		String minutes = request.getFirstChild( "cronSpecs" ).getFirstChild( "minute" ).strValue();
		String hours = request.getFirstChild( "cronSpecs" ).getFirstChild( "hour" ).strValue();
		String dayOfMonth = request.getFirstChild( "cronSpecs" ).getFirstChild( "dayOfMonth" ).strValue();
		String month = request.getFirstChild( "cronSpecs" ).getFirstChild( "month" ).strValue();
		String dayOfWeek = request.getFirstChild( "cronSpecs" ).getFirstChild( "dayOfWeek" ).strValue();
		String year = "";
		if( request.getFirstChild( "cronSpecs" ).getFirstChild( "year" ).isDefined() ) {
			year = " " + request.getFirstChild( "cronSpecs" ).getFirstChild( "year" ).strValue();
		}


		JobDetail job = JobBuilder.newJob( SchedulerServiceJob.class )
			.withIdentity( jobName, groupName ).build();

		Trigger trigger = TriggerBuilder.newTrigger()
			.withIdentity( jobName, groupName )
			.startNow().withSchedule( CronScheduleBuilder.cronSchedule(
				seconds + " " + minutes + " " + hours + " " + dayOfMonth + " " + month + " " + dayOfWeek + year ) )
			.forJob( jobName, groupName ).build();


		try {
			if( scheduler.checkExists( trigger.getJobKey() ) ) {
				throw new FaultException( "JobAlreadyExists" );
			}
			scheduler.scheduleJob( job, trigger );
		} catch( SchedulerException ex ) {
			Logger.getLogger( SchedulerService.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return Value.create();
	}

	@RequestResponse
	public Value deleteCronJob( Value request ) {
		String jobName = request.getFirstChild( "jobName" ).strValue();
		String groupName = request.getFirstChild( "groupName" ).strValue();
		try {
			if( scheduler.checkExists( TriggerKey.triggerKey( jobName, groupName ) ) ) {
				scheduler.unscheduleJob( TriggerKey.triggerKey( jobName, groupName ) );
				scheduler.deleteJob( JobKey.jobKey( jobName, groupName ) );
			}
		} catch( SchedulerException ex ) {
			Logger.getLogger( SchedulerService.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return Value.create();
	}

}
