/*
 *   Copyright (C) 2008-2019 by Fabrizio Montesi <famontesi@gmail.com>     
 *   Copyright (C) 2013      by Claudio Guidi    <guidiclaudio@gmail.com>  
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


include "types/JavaException.iol"
include "types/IOException.iol"
include "types/Binding.iol"

type LoadEmbeddedServiceRequest:void {
	.filepath:string //< The path to the service to load
	.type:string //< The type of the service, e.g., Jolie, Java, or JavaScript
}
| void {
	.code: string //< the jolie code to be embedded
}

type GetRedirectionRequest:void {
	.inputPortName:string //< The target input port
	.resourceName:string //< The resource name of the redirection to get
}

type SetRedirectionRequest:void {
	.inputPortName:string //< The target input port
	.resourceName:string //< The target resource name
	.outputPortName:string //< The target output port
}

type RuntimeExceptionType:JavaExceptionType

type SetOutputPortRequest:void {
	.name:string //< The name of the output port
	.location:any //< The location of the output port
	/// The protocol configuration of the output port
	.protocol?:string //< The name of the protocol (e.g., sodep, http)
		{ ? }
}

type SendMessageRequest:void {
	.operation:string //< The operation the message is for
	.binding:Binding //< The binding information (location, protocol) to reach the target service
	.message:undefined //< The message content (payload)
}

type GetIncludePathResponse:void {
	.path*:string //< The include paths of the interpreter
}

type SetMonitorRequest:void {
	.location:any //< The location of the monitor
	/// The protocol configuration for the monitor
	.protocol?:string { ? }
}

type GetOutputPortRequest: void {
	.name: string //< The name of the output port
}

type GetOutputPortResponse: void {
	.name: string //< The name of the output port
	.protocol: string //< The protocol name of the output port
	.location: string //< The location of the output port
}

type GetOutputPortsResponse: void {
	/// The output ports used by this interpreter
	.port*: void {
	  .name: string //< The name of the output port
	  .protocol: string //< The protocol name of the output port
	  .location: string //< The location of the output port
	}
}

type HaltRequest: void {
	.status?: int //< The status code to return to the execution environment
}

/// the message to be logged
type LogMonitorRequest: string {
	.level?: string //< INFO (default) | WARNING | ERROR
}

/// Information on the interpreter execution so far
type Stats:void {
	/// Information on file descriptors
	.files:void {
		.openCount?:long //< Number of open files
		.maxCount?:long //< Maximum number of open files allowed for this VM
	}
	/// OS-related information
	.os:void {
		.arch:string //< Architecture
		.availableProcessors:int //< Number of available processors
		.name:string //< Name of the OS
		.systemLoadAverage:double //< System load average
		.version:string //< OS version
	}
}

type MaybeString:void | string

interface RuntimeInterface {
RequestResponse:
	/// Get the local in-memory location of this service.
	getLocalLocation(void)(any),

	/// Set the monitor for this service.
	setMonitor(SetMonitorRequest)(void),

	/// Load an embedded service.
	loadEmbeddedService(LoadEmbeddedServiceRequest)(any) throws RuntimeException(RuntimeExceptionType),
	
	/// send a log to the registered monitor
	log( LogMonitorRequest )( void ),

	/// Get the output port name that a redirection points to.
	getRedirection(GetRedirectionRequest)(MaybeString),

	/** Set a redirection at an input port.
	 * If the redirection with this name does not exist already,
	 * this operation creates it.
	 * Otherwise, the redirection is replaced with this one.
	 */
	setRedirection(SetRedirectionRequest)(void) throws RuntimeException(RuntimeExceptionType),

	/// Remove a redirection at an input port
	removeRedirection(GetRedirectionRequest)(void) throws RuntimeException(RuntimeExceptionType),

	/// Get the include paths used by this interpreter
	getIncludePaths(void)(GetIncludePathResponse),

	/** Set an output port.
	 * If an output port with this name does not exist already,
	 * this operation creates it.
	 * Otherwise, the output port is replaced with this one.
	 */
	setOutputPort(SetOutputPortRequest)(void),

	/** Returns the definition of output port definition.
	 * @throws OutputPortDoesNotExist if the requested output port does not exist.
	 */
	getOutputPort( GetOutputPortRequest )( GetOutputPortResponse )
	  throws OutputPortDoesNotExist,

	/// Returns all the output ports used by this service.
	getOutputPorts( void )( GetOutputPortsResponse ),

	/// Returns the internal identifier of the executing Jolie process.
	getProcessId( void )( string ),

	/// Halts non-gracefully the execution of this service.
	halt(HaltRequest)(void),

	/// Removes the output port with the requested name.
	removeOutputPort(string)(void),

	/** Stops gracefully the execution of this service.
	 * Calling this operation is equivalent to invoking the exit statement.
	 */
	callExit(any)(void),

	/** Returns a pretty-printed string representation of
	 * the local state of the invoking Jolie process and
	 * the global state of this service.
	 */
	dumpState(void)(string),

	/// Dynamically loads an external (jar) library.
	loadLibrary(string)(void) throws IOException(IOExceptionType),

	/// Returns information on the runtime state of the VM.
	stats(void)(Stats),

	/// Returns the value of an environment variable.
	getenv(string)(MaybeString),

	/// Returns the version of the Jolie interpreter running this service.
	getVersion(void)(string)
}

outputPort Runtime {
Interfaces: RuntimeInterface
}

embedded {
Java:
	"joliex.lang.RuntimeService" in Runtime
}
