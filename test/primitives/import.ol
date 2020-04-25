from .private.imports.point import point
from .private.imports.point import point as p
// from .private.imports.iface import fooIface
from .private.imports.procedure import proc
from .private.imports.namespace import *
from .private.imports.namespace import n1 as asN1, n2 as asN2
from .private.twice import TwiceAPI
from t import test
from bar.foo import type_foo, type_bar, type_bar_package
include "../AbstractTestUnit.iol"

define doTest {
    v << {x= 1, y= 2}

	if ( !(v instanceof point) ) {
		throw( TestFailed, "point is not imported" )
	}

	// test qualified name
	if ( !(v instanceof p) ) {
		throw( TestFailed, "point is not imported" )
	}

	// // test interface
	// f << { a = "test" }
	// if ( !(f instanceof foo) ) {
	// 	throw( TestFailed, "foo type is not imported with fooIface" )
	// }

	// test procedure
	proc

	if ( !(p_proc instanceof point) ) {
		throw( TestFailed, "proc was not called" )
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