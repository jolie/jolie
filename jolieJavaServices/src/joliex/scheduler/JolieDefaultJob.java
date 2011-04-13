/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.scheduler;

import jolie.net.CommMessage;
import jolie.runtime.Value;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author Francesco
 */
public class JolieDefaultJob implements Job
{
	@Override
	public void execute( JobExecutionContext context )
		throws JobExecutionException
	{

		JobDataMap data = context.getMergedJobDataMap();
		String callbackOperation = data.getString( "operation" );
		SchedulerService jsched = (SchedulerService) data.get( "javaSchedulerService" );
		//System.out.println("callbackOperation = " + );
		Value callbackValue = Value.create();
		jsched.sendMessage( CommMessage.createRequest( callbackOperation, "/", callbackValue ) );
	}
}
