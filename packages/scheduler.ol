/*
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>         
 *                 2018 by Claudio Guidi <cguidi@italianasoftware.com>    
 *                                                                        
 *   This program is free software; you can redistribute it and/or modify 
 *   it under the terms of the GNU Library General Public License as      
 *   published by the Free Software Foundation; either version 2 of the   
 *   License, or (at your option) any later version.                      
 *                                                                        
 *   This program is distributed in the hope that it will be useful,      
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of       
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        
 *   GNU General Public License for more details.                         
 *                                                                        
 *   You should have received a copy of the GNU Library General Public    
 *   License along with this program; if not, write to the                
 *   Free Software Foundation, Inc.,                                      
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            
 *                                                                        
 *   For details about the authors of this software, see the AUTHORS file.
 */

type DeleteCronJobRequest: void {
  .jobName: string
  .groupName: string
}

type SetCallBackOperationRequest: void {
  .operationName: string
}

type SetCronJobRequest: void {
  .jobName: string
  .groupName: string
  .cronSpecs: void {
      /* see here for creating correct specs:
         http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/tutorial-lesson-06.html
         or http://www.cronmaker.com/
      */
         .second: string   		/* 0-59 */
         .minute: string				/* 0-59 */
         .hour: string					/* 0-23 */
         .dayOfMonth: string		/* 1-31 */
         .month: string				/* 0-11 */
         .dayOfWeek: string		/* 1-7 (1=Sunday) */
         .year?: string
  }
}

interface SchedulerInterface{
   // Delete an existing cron job
   deleteCronJob( DeleteCronJobRequest )( void ),

   /// Set a new cron job
   setCronJob( SetCronJobRequest )( void ) throws JobAlreadyExists( void )
   OneWay:
   /// Set the callback operation name
   setCallbackOperation( SetCallBackOperationRequest )
}

service Scheduler {
    inputPort ip {
        location:"local"
        interfaces: SchedulerInterface
    }

    foreign java {
        class: "joliex.scheduler.SchedulerService"
    }
}