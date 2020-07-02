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

interface MonitorInterface {
OneWay:
	pushEvent(undefined)
}

type LogEventData: void {
	level: string
	message: string 
	processId: string
	extendedType?: string
}

type OperationCallReplyEventData: void {
	operationName: string
	processId: string 
	messageId: string 
	status: int 
	details: string 
	outputPort: string 
	value: undefined
}

type OperationEndedEventData: void {
	operationName: string
	processId: string 
	messageId: string 
	status: int 
	details: string 
	value: undefined
}

type OperationStartedEventData: void {
	operationName: string
	processId: string 
	messageId: string 
	value: undefined
}

type ProtocolMessageEventData: void {
	protocol: string 
	header: string 
	message: string
	processId: string  
}

type SessionEventData: void {
	operationName: string
	processId: string 
}

type MonitorAttachedEventData: void


type MonitorEvent: void {
	type: string 
	memory: long
	timestamp: long
	service: string
	cellId: int
	scope: string
	data?: LogEventData 
		| OperationCallReplyEventData 
		| OperationEndedEventData 
		| OperationStartedEventData
		| ProtocolMessageEventData
		| SessionEventData
		| MonitorAttachedEventData
	context?: void {
		filename: string 
		line: int
	}
}

type FlushResponse: void {
	.events*: MonitorEvent
}

type SetStandardMonitorRequest: void {
	.triggeredEnabled?: bool
	.triggerThreshold?: int
	.queueMax?: int
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

