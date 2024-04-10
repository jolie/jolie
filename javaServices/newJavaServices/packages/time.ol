/*
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          
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


/**
*WARNING: work in progress, the API is unstable.
*/


type DateValuesType:void {
	.day:int
	.month:int
	.year:int
}

type TimeValuesType:void {
	.hour:int
	.minute:int
	.second:int
}

type DateValuesRequestType:string {
	/*
	* Date format.
	* If not specified, it defaults to "dd/MM/yyyy"
	*/

	.format?:string
}

type CurrentDateTimeRequestType:void {
	/*
	* 	Date format.
	* 	If not specified, it defaults to "dd/MM/yyyy"
	*/

	.format?:string
}

type DiffDateRequestType:void {
	.format?:string
	.date1:string
	.date2:string
}
type GetTimeDiffRequest:void {
	.time1:string
	.time2:string
}

type GetTimestampFromStringRequest:string {
	.format?:string
	.language?: string
}

type GetDateTimeRequest: long {
  .format?: string

}

type GetDateTimeResponse: string {
	.day:int
	.month:int
	.year:int
	.hour:int
	.minute:int
	.second:int
}

type DateTimeType:void{
	.day:int
	.month:int
	.year:int
	.hour:int
	.minute:int
	.second:int
}


type SetNextTimeOutRequest: int {
	.operation?: string
	.message?: undefined
}

type ScheduleTimeOutRequest: int {
	.operation?: string
	/*
	* If the value is not set, it will default to "timeout"
	*
	*/
	.message?: undefined
	/*
	*	Possible values are DAYS, HOURS, MICROSECONDS, MILLISECONDS, MINUTES, NANOSECONDS, SECONDS.
	*   If the value is not set or recognized, it will default to MILLISECONDS
	*/
	.timeunit?: string
}

interface TimeInterface{
	OneWay:
		/**!
		  it sets a timeout whose duration is in milliseconds and it is represented by the root value of the message
		  When the alarm is triggered a message whose content is defined in .message is sent to operation defined in .operation
		  ( default: timeout )
		*/
		setNextTimeout(SetNextTimeOutRequest),
		setNextTimeoutByDateTime, setNextTimeoutByTime,
		/**! It stops the current timeout previously set with a setNextTimeout */
		stopNextTimeout( void )
	RequestResponse:
		/**!
		It Cancels a timeout from a long-value created from #scheduleTimeout
		*/
		cancelTimeout(long)(bool),

		/**!
		It returns the current date time as a string
		*/
		getCurrentDateTime(CurrentDateTimeRequestType)(string), 
		
		/**!
		It waits for a period specified in the request (in milliseconds)
		*/
		sleep( int )( void ),

		/**!
		* It returns a date time in a string format starting from a timestamp
		*/
		getDateTime( GetDateTimeRequest )( GetDateTimeResponse ),

		/**!
		* Converts an input string into a date expressed by means of
		* three elements: day, month and year. The request may specify the
		* date parsing format. See #DateValuesRequestType for details.
		*/
		getDateValues(DateValuesRequestType)(DateValuesType) throws InvalidDate,

		/**!
		* Returns the current date split in three fields: day, month and year
		*/
		getCurrentDateValues(void)(DateValuesType),
		getDateDiff(DiffDateRequestType)(int),

		/**!
		* Warning: this is temporary and subject to future change as soon as long is supported by Jolie.
		*/
		getCurrentTimeMillis(void)(long),
		getTimeValues(string)(TimeValuesType),
		getTimeDiff(GetTimeDiffRequest)(int),
		getTimeFromMilliSeconds(int)(TimeValuesType),
		getTimestampFromString(GetTimestampFromStringRequest)(long) throws InvalidTimestamp,
		getDateTimeValues(GetTimestampFromStringRequest)(DateTimeType) throws InvalidDate,
		/**!
		* Schedules a timeout, which can be cancelled using #cancelTimeout from the returned string. Default .timeunit value is MILLISECONDS, .operation default is "timeout".
		*/
		scheduleTimeout(ScheduleTimeOutRequest)(long) throws InvalidTimeUnit

}

service Time {
    inputPort ip {
        location:"local"
        interfaces: TimeInterface
    }

    foreign java {
        class: "joliex.util.NewTimeService"
    }
}