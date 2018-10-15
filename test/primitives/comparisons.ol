/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

include "../AbstractTestUnit.iol"

include "security_utils.iol"

define doTest
{
	x = true;
	if ( x != true || true != x ) {
		throw( TestFailed, "bool: variable x does not match its own value" )
	};
	if ( true != true || x != x || x != bool(x) ) {
		throw( TestFailed, "bool: values do not match" )
	};
	if ( x != 1 || x != 1L || x != 1.0 || x != "true" ) {
		throw( TestFailed, "bool: conversion problems" )
	};
	if ( x != int(true) || x != long(true) || x != double(true) || x != string(true) ) {
		throw( TestFailed, "bool: conversion problems" )
	};

	x = 1;
	if ( x != 1 || 1 != x ) {
		throw( TestFailed, "int: variable x does not match its own value" )
	};
	if ( 1 != 1 || x != x || x != int(x) ) {
		throw( TestFailed, "int: values do not match" )
	};
	if ( x != true || x != 1L || x != 1.0 || x != "1" ) {
		throw( TestFailed, "int: conversion problems" )
	};
	if ( x != bool(1) || x != long(1) || x != double(1) || x != string(1) ) {
		throw( TestFailed, "int: conversion problems" )
	};

	x = 1L;
	if ( x != 1L || 1L != x ) {
		throw( TestFailed, "long: variable x does not match its own value" )
	};
	if ( 1L != 1L || x != x || x != long(x) ) {
		throw( TestFailed, "long: values do not match" )
	};
	if ( x != true || x != 1 || x != 1.0 || x != "1" ) {
		throw( TestFailed, "long: conversion problems" )
	};
	if ( x != bool(1L) || x != int(1L) || x != double(1L) || x != string(1L) ) {
		throw( TestFailed, "long: conversion problems" )
	};

	x = 1.0;
	if ( x != 1.0 || 1.0 != x ) {
		throw( TestFailed, "double: variable x does not match its own value" )
	};
	if ( 1.0 != 1.0 || x != x || x != double(x) ) {
		throw( TestFailed, "double: values do not match" )
	};
	if ( x != true || x != 1 || x != 1L || x != "1" ) {
		throw( TestFailed, "int: conversion problems" )
	};
	if ( x != bool(1.0) || x != int(1.0) || x != long(1.0) || x != string(1.0) ) {
		throw( TestFailed, "int: conversion problems" )
	};

	x = "Döner";
	if ( x != "Döner" || "Döner" != x ) {
		throw( TestFailed, "string: variable x does not match its own value" )
	};
	if ( "Döner" != "Döner" ) {
		throw( TestFailed, "string: values do not match" )
	};
	if ( "" != "" || x != x  || x != string(x) ) {
		throw( TestFailed, "string: empty strings do not match" )
	};
	if ( "true" != true  || "1" != 1 || "1.0" == 1 || "1" != 1L || "1.0" == 1 || "1.0" != 1.0 || "1" == 1.0 ) {
		throw( TestFailed, "string: conversion problems" )
	};

	req.size = 50;
	secureRandom@SecurityUtils(req)(x);
	if ( x != x ) {
		throw( TestFailed, "raw: values do not match" )
	};

	a = 1;
	a.a = 2;
	b = 1;
	b.a = 3;
	if ( a != b ) {
		throw( TestFailed, "compound: root values do not match" )
	};
	if ( a.a == b.a ) {
		throw( TestFailed, "compound: child values do match" )
	}
}

