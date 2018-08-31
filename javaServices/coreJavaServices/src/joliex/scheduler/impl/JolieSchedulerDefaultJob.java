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

package joliex.scheduler.impl;

import jolie.net.CommMessage;
import jolie.runtime.Value;
import joliex.scheduler.SchedulerService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 *
 * @author Francesco Bullini
 */
public class JolieSchedulerDefaultJob implements Job
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
