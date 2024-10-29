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

/// A string with a text location.
type LocatedString: string { textLocation: TextLocation }

/// A reference to the definition of a symbol.
type SymbolRef: string

/// A symbol reference with a text location.
type LocatedSymbolRef: string { textLocation: TextLocation }

/// A documentation node.
type Documentation: LocatedString

/// An integer basic type.
type IntBasicType {
	int
	refinements*: IntRefinement
}

type IntRefinement {
	ranges*: IntRange
}

/// A long basic type.
type LongBasicType {
	long
	refinements*: LongRefinement
}

type LongRefinement {
	ranges*: LongRange
}

type LongRange {
	min: long
	max: long
}

/// A double basic type.
type DoubleBasicType {
	double
	refinements*: DoubleRefinement
}

type DoubleRefinement {
	ranges*: DoubleRange
}

type DoubleRange {
	min: double
	max: double
}

/// A string basic type.
type StringBasicType {
	string
	refinements*: StringRefinement
}

type StringRefinement:
	{ length: IntRange }
	|
	{ enum[1,*]: string }
	|
	{ regex: string }

/// A boolean basic type.
type BoolBasicType { bool }

/// A void basic type.
type VoidBasicType { void }

/// A range, as used in types.
type IntRange {
	min: int //< Cannot be lower than 0 and should always be lower than or equal to max.
	max: int
}

/// A basic type.
type BasicType: // TODO (lacks refinements)
	VoidBasicType
	|
	BoolBasicType
	|
	IntBasicType
	|
	LongBasicType
	|
	DoubleBasicType
	|
	StringBasicType

/// A tree type.
type TreeType {
	textLocation: TextLocation
	documentation?: Documentation
	basicType: BasicType
	nodes* {
		textLocation: TextLocation
		name: LocatedString
		documentation?: Documentation
		range: Range
		type: Type
	}
	wildcard?: Type // TODO: discussion in progress.
}

/// A choice type.
type ChoiceType {
	textLocation: TextLocation
	left: Type
	right: Type
}

/// A type. // TODO: makes switching hard, consider defining the possible options as their own types.
type Type:
	{ tree: TreeType }
	|
	{ choice: ChoiceType }
	|
	{ ref: LocatedSymbolRef	}

/// A type definition.
type TypeDef {
	textLocation: TextLocation
	name: LocatedString
	type: Type
}

/// An aggregation, as in the aggregates construct used in input ports.
type Aggregation {
	textLocation: TextLocation
	outputPort: LocatedSymbolRef
	extender*: LocatedSymbolRef
}

/// The type of a OneWay operation.
type OneWayOperation {
	textLocation: TextLocation
	name: LocatedString
	requestType: Type
}

/// The type of a fault.
type FaultType {
	textLocation: TextLocation
	name: LocatedString
	type: Type
}

/// The type of a RequestResponse operation.
type RequestResponseOperation {
	textLocation: TextLocation
	name: LocatedString
	requestType: Type
	responseType: Type
	faults*: FaultType
}

/// The type of an operation.
type Operation: OneWayOperation | RequestResponseOperation

/// A redirection, as used in an input port.
type Redirection {
	textLocation: TextLocation
	name: LocatedString
	outputPort: LocatedSymbolRef
}

/// An input port.
type InputPort {
	textLocation: TextLocation
	name: LocatedString
	location?: LocatedString
	protocol?: LocatedString
	operations*: Operation
	interfaces*: SymbolRef
	aggregations*: Aggregation
	redirections*: Redirection
}

/// An output port.
type OutputPort {
	textLocation: TextLocation
	name: LocatedString
	location?: LocatedString
	protocol?: LocatedString
	operations*: Operation
	interfaces*: SymbolRef
}

/// A service.
type Service {
	textLocation: TextLocation
	documentation?: Documentation
	name: LocatedString
	inputPorts*: InputPort
	outputPorts*: OutputPort
}

/// An interface definition.
type InterfaceDef {
	textLocation: TextLocation
	name: LocatedString
	operations*: Operation
}

/// A module.
type Module {
	types*: TypeDef
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