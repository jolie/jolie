/*
 *   Copyright (C) 2020 by Claudio Guidi <cguidi@italianasoftware.com>    
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

type SetOpenTracingMonitorRequest: void {
    service_name?: string                            //> name of the service which appears in the search (default:service)
    session_ended_timeout_before_elaboration?: long  //> number of milliseconds to wait after a session_ended event is received before elaborating spans. Since events are asynchonously received by the monitor, it is necessary for collecting all the events of a given session. (default: 10000)
    tracer_with_log_spans?: bool                     //> enable or not the logs of the spans in the console (default: false)
}


interface MonitorInterface {
RequestResponse:
	setMonitor( SetOpenTracingMonitorRequest )( void ) 
OneWay:
	pushEvent(undefined)
}

outputPort Monitor {
	Interfaces: MonitorInterface
}


embedded {
Java:
	"joliex.monitoring.OpenTracingMonitor" in Monitor
}