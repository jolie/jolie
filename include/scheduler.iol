/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
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


type FullDate:void {
	.msec:int
	.sec:int
	.min:int
	.hour:int	
	.day:int
	.month:int
	.year:int
}


/**!
* Configure the schedule with a repeat-period end a start time
*/
type ScheduleReq:void {

	.format?:string    //If not specified, it defaults to "dd/MM/yyyy"
	.operation?:string //if not specified, it defaults to timeout
	.start_date:FullDate
	.period:int
}

/**!
* Configure the schedule for once daily activation
*/
type DailyScheduleReq:void {

	.operation?:string //if not specified, it defaults to timeout
	.start_hour:int    //if not specified, it defaults to 21
	.start_min?:int    //if not specified, it defaults to 0
}

/**!
* Configure the schedule in chron style string
*/

/*
*type CronScheduleReq:void {
*
*	.operation?:string   //if not specified, it defaults to timeout
*	.chron_config:string //example "0 42 10 * * ?" ogni giorno alle 10:42
*	.date_format?:string
*	//.start_date?:FullDate //TODO if not specified, it defaults to now
*	//.end_date?:FullDate   //TODO if not specified, it defaults to never
*}
*/

interface SchedulerInterface{
	OneWay:
		setSchedule( ScheduleReq ),
		//,setScheduleByCronFormat( ChronScheduleReq )
		setDailySchedule( DailyScheduleReq )
}

outputPort SchedulerPort {
	Interfaces: SchedulerInterface
}

embedded {
Java:
	"joliex.scheduler.SchedulerService" in SchedulerPort
}
