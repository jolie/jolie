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
from .private.imports.@pkg import mainDefaultType
from twice.twice.main import TwiceAPI
from .packages.t import test
from .packages.bar.foo import type_foo, type_bar, type_bar_package
from .packages.service import serviceA
from ..test-unit import TestUnitInterface
from .private.import_server_old import Main as s1
from .private.import_server import main as s2
from .private.import_default import defaultService as s3
from finder.A import A
from finder.B import B

service Main {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

	embed A as A
	embed B as B
	embed s1 as s1
	embed s2 as s2
	embed s3 as s3

	main {
		test()() {
			v << {x= 1, y= 2}

			if ( !(v instanceof point) ) {
				throw( TestFailed, "point is not imported" )
			}

			// test qualified name
			if ( !(v instanceof p) ) {
				throw( TestFailed, "point is not imported" )
			}

			// test for old syntax
			barOp@s1({b = "somestr"})
			fooOp@s1({a = "somestr"})(res)
			if ( res.b != "success" ) {
				throw( TestFailed, "import interface is not correctly import" )
			}

			scope( testImportIface ) {
				install( err => nullProcess)
				fooOp@s1({a = "err"})(res)
				if (is_defined(res)){
					throw( TestFailed, "expected fault" )
				}
			}

			// test for new syntax
			barOp@s2({b = "somestr"})
			fooOp@s2({a = "somestr"})(res)
			if ( res.b != "success" ) {
				throw( TestFailed, "import interface is not correctly import" )
			}

			// test for new syntax
			barOp@s3({b = "somestr"})
			fooOp@s3({a = "somestr"})(res)
			if ( res.b != "success" ) {
				throw( TestFailed, "import interface is not correctly import" )
			}

			scope( testImportIface ) {
				install( err => nullProcess)
				fooOp@s2({a = "err"})(res)
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

			// test default to main.ol
			m << {zz= "1"}

			if ( !(m instanceof mainDefaultType) ) {
				throw( TestFailed, "mainDefaultType is not imported" )
			}

			// test server
			version@A()(res)
			if ( res != "1.0.0" ) {
				throw( TestFailed, "version@A expected 1.0.0, got " + res )
			}
			version@B()(res)
			if ( res != "1.1.0" ) {
				throw( TestFailed, "version@B expected 1.1.0, got " + res )
			}
		}
	}
}