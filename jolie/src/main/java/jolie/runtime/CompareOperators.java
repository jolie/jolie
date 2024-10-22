/***************************************************************************
 *   Copyright 2009-2015 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.runtime;

import java.util.function.BiPredicate;

/**
 * Boolean comparison operators for {@link Value} objects.
 *
 * @author Fabrizio Montesi
 */
public final class CompareOperators {

	public final static BiPredicate< Value, Value > EQUAL = Value::isEqualTo;
	// ( left, right ) -> left.isEqualTo( right );
	public final static BiPredicate< Value, Value > NOT_EQUAL = EQUAL.negate();
	public final static BiPredicate< Value, Value > MINOR =
		( left, right ) -> {
			if( left.isDouble() || right.isDouble() ) {
				return (left.doubleValue() < right.doubleValue());
			}
			if( left.isLong() || right.isLong() ) {
				return (left.longValue() < right.longValue());
			} else {
				return (left.intValue() < right.intValue());
			}
		};
	public final static BiPredicate< Value, Value > MAJOR =
		( left, right ) -> {
			if( left.isDouble() || right.isDouble() ) {
				return (left.doubleValue() > right.doubleValue());
			}
			if( left.isLong() || right.isLong() ) {
				return (left.longValue() > right.longValue());
			} else {
				return (left.intValue() > right.intValue());
			}
		};
	public final static BiPredicate< Value, Value > MINOR_OR_EQUAL =
		( left, right ) -> {
			if( left.isDouble() || right.isDouble() ) {
				return (left.doubleValue() <= right.doubleValue());
			}
			if( left.isLong() || right.isLong() ) {
				return (left.longValue() <= right.longValue());
			} else {
				return (left.intValue() <= right.intValue());
			}
		};
	public final static BiPredicate< Value, Value > MAJOR_OR_EQUAL =
		( left, right ) -> {
			if( left.isDouble() || right.isDouble() ) {
				return (left.doubleValue() >= right.doubleValue());
			}
			if( left.isLong() || right.isLong() ) {
				return (left.longValue() >= right.longValue());
			} else {
				return (left.intValue() >= right.intValue());
			}
		};
}
