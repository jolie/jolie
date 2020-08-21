/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
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

from .private.imports.point import point
from .private.imports.point import point as p
from .private.imports.iface import fooIface
from .private.imports.namespace import *
from .private.imports.namespace import n1 as asN1, n2 as asN2
from twice.twice.main import TwiceAPI
from .packages.t import test
from .packages.bar.foo import type_foo, type_bar, type_bar_package
from .packages.service import serviceA
include "../AbstractTestUnit.iol"

outputPort Server {
	interfaces: fooIface
}

embedded {
Jolie:
	"private/import_server.ol" in Server
}

define doTest {
    v << {x= 1, y= 2}

	if ( !(v instanceof point) ) {
		throw( TestFailed, "point is not imported" )
	}

	// test qualified name
	if ( !(v instanceof p) ) {
		throw( TestFailed, "point is not imported" )
	}

	// test interface
	barOp@Server({b = "somestr"})
	fooOp@Server({a = "somestr"})(res)
	if ( res.b != "success" ) {
		throw( TestFailed, "import interface is not correctly import" )
	}

	scope( testImportIface ) {
		install( err => nullProcess)
		fooOp@Server({a = "err"})(res)
		if (is_defined(res)){
			throw( TestFailed, "expected fault" )
		}
	}

	// test namespace
	n1_val << { n1_f = 1}
	n2_val << { n2_f = "t"}
	if ( !(n1_val instanceof n1) ) {
		throw( TestFailed, "n1 is not imported" )
	}
	if ( !(n2_val instanceof n2) ) {
		throw( TestFailed, "n2 is not imported" )
	}

	// test import multiple symbol
	if ( !(n1_val instanceof asN1) ) {
		throw( TestFailed, "asN1 is not imported" )
	}
	if ( !(n2_val instanceof asN2) ) {
		throw( TestFailed, "asN2 is not imported" )
	}

	if ( !("t" instanceof test) ) {
		throw( TestFailed, "test is not imported" )
	}

	if ( !(1 instanceof type_foo) ) {
		throw( TestFailed, "type_foo is not imported" )
	}

	if ( !("str" instanceof type_bar) ) {
		throw( TestFailed, "type_bar is not imported" )
	}

	bar_pack_val << { bar_sub = "str"}
	if ( !(bar_pack_val instanceof type_bar_package) ) {
		throw( TestFailed, "type_bar_package is not imported" )
	}
}