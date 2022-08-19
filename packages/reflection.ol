/*
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>         
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

from types.Binding import Binding

type InvokeRequest:void {
	.operation:string 
	.outputPort:string
	.resourcePath?:string
	.data?:undefined
}

type InvocationFaultType:void {
	.name:string
	.data:undefined
}

/*
type Range:void { .min:int .max:int } // Both extremes are included

type NativeType
	: string("void")
	| string("int")
	| string("string")
	| string("double")
	| string("long")
	| string("raw")

type Type:void {
	.name:string
	.nativeType:NativeType
	.range:Range
	.subTypes*:Type
}

type OneWayOperation:void {
	.name:string
	.requestType:Type
}

type FaultType:void {
	.name:string
	.type:Type
}

type RequestResponseOperation:void {
	.name:string
	.requestType:Type
	.responseType:Type
	.faultType*:FaultType
}

type Operation:OneWayOperation | RequestResponseOperation

type Interface:void {
	.name:string
	.operation*:Operation
}
*/

type ReflectionSetOutputPortRequest:void {
	.name:string
	.binding:Binding
}

/**!
WARNING: the API of this service is experimental. Use it at your own risk.
*/
interface ReflectionIface {
RequestResponse:
	/**!
	Invokes the specified operation at outputPort.
	If the operation is a OneWay, the invocation returns no value.
	*/
	invoke(InvokeRequest)(undefined) throws OperationNotFound(string) InvocationFault(InvocationFaultType),
	invokeRRUnsafe(InvokeRequest)(undefined) throws InvocationFault(InvocationFaultType)
}

service Reflection {
    inputPort ip {
        location:"local"
        interfaces: ReflectionIface
    }

    foreign java {
        class: "joliex.lang.reflection.Reflection"
    }
}