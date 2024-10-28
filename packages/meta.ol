/*
 * Copyright (C) 2024 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

from types.text import Location as TextLocation

/// A reference to the definition of a symbol.
type SymbolRef: string

/// An aggregation, as in the aggregates construct used in input ports.
type Aggregation {
	outputPort: SymbolRef
	extender*: SymbolRef
}

/// The type of a OneWay operation.
type OneWayOperation {
	name: string
	requestType: SymbolRef
}

/// The type of a fault.
type FaultType {
	name: string
	type: SymbolRef
}

/// The type of a RequestResponse operation.
type RequestResponseOperation {
	name: string
	requestType: SymbolRef
	responseType: SymbolRef
	faults*: FaultType
}

/// The type of an operation.
type Operation: OneWayOperation | RequestResponseOperation

/// A redirection, as used in an input port.
type Redirection {
	name: string
	outputPort: SymbolRef
}

/// An input port.
type InputPort {
	name: string
	location?: string
	protocol?: string
	operations*: Operation
	interfaces*: SymbolRef
	aggregations*: Aggregation
	redirections*: Redirection
}

/// An output port.
type OutputPort {
	textLocation: TextLocation
	name: string
	location?: string
	protocol?: string
	operations*: Operation
	interfaces*: SymbolRef
}

/// A service.
type Service {
	textLocation: TextLocation
	name: string
	inputPorts*: InputPort
	outputPorts*: OutputPort
}

/// An interface definition.
type InterfaceDef {
	name: string
	operations*: Operation
}

/// A module.
type Module {
	types*: void // TODO
	interfaces*: InterfaceDef
	services*: Service
}

interface MetaInterface {
RequestResponse:
	parseModule( string )( Module )
}

service Meta {
	inputPort input {
		location: "local"
		interfaces: MetaInterface
	}
	foreign java {
		class: "joliex.meta.MetaService"
	}
}