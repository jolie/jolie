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

define doTest
{
	undef( x )
	if( #x != 0 ) {
		throw( TestFailed, "Undefined variable size is not zero" )
	}

	if( is_defined( x ) ) {
		throw( TestFailed, "Vector size operator defines an undefined variable" )
	}

	x = true
	if( !is_defined( x ) ) {
		throw( TestFailed, "Variable has been defined" )
	}

	x = undefined
	if( !is_defined( x ) ) {
		throw( TestFailed, "Variable has been defined to be void" )
	}
	if( is_defined( undefined ) ) {
		throw( TestFailed, "Constant has been defined" )
	}

	x = any
	if ( !is_defined( x ) ) {
		throw( TestFailed, "Variable has been defined to be void" )
	}
	if ( is_defined( any ) ) {
		throw( TestFailed, "Constant has been defined" )
	}

	x = void
	if ( !is_defined( x ) ) {
		throw( TestFailed, "Variable has been defined to be void" )
	}
	if ( is_defined( void ) ) {
		throw( TestFailed, "Constant has been defined" )
	}
}

