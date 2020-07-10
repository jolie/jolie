/*
 *   Copyright (C) 2012 by Claudio Guidi <cguidi@italianasoftware.com>    
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

include "monitors/types/monitor_types.iol"

interface MonitorInterface {
OneWay:
	pushEvent(undefined)
}


type FlushResponse: void {
	events*: MonitorEvent
}

type SetStandardMonitorRequest: void {
	queueMax?: int							//< internal queue size which collects the events from the engine (default:100)
	triggeredEnabled?: bool					//< enable trigger from the monitor to the embedder which will send collected events to the operation getMonitorEvent (default: false)
	triggerThreshold?: int					//< the size of the the queue which triggers the getMonitorEvent of the events. Keep it less than queueMax in order to not loose events
}

interface StandardMonitorInterface {
RequestResponse:
	flush( void )( FlushResponse ),
	setMonitor( SetStandardMonitorRequest )( void )
}

interface StandardMonitorInputInterface {
OneWay:
	monitorAlert( void )
}

outputPort Monitor {
	Interfaces: MonitorInterface, StandardMonitorInterface
}

inputPort MonitorInput {
	Location: "local"
	Interfaces: StandardMonitorInputInterface
}

embedded {
Java:
	"joliex.monitoring.StandardMonitor" in Monitor
}

