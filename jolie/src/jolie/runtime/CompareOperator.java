/***************************************************************************
 *   Copyright 2009 (C) by Fabrizio Montesi                                     *
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

/**
 * A CompareOperator performs some kind of boolean comparison between two values.
 * @author Fabrizio Montesi
 */
public enum CompareOperator
{
	EQUAL {
		public final boolean evaluate( Value left, Value right ) {
			return left.equals( right );
		}
	}, NOT_EQUAL {
		public final boolean evaluate( Value left, Value right ) {
			return EQUAL.evaluate( left, right ) == false;
		}
	}, MINOR {
		public final boolean evaluate( Value left, Value right ) {
			if ( left.isDouble() ) {
				return ( left.doubleValue() < right.doubleValue() );
			} if ( left.isLong() ) {
				return ( left.longValue() < right.longValue() );
			} else {
				return ( left.intValue() < right.intValue() );
			}
		}
	}, MAJOR {
		public final boolean evaluate( Value left, Value right ) {
			if ( left.isDouble() ) {
				return ( left.doubleValue() > right.doubleValue() );
			} if ( left.isLong() ) {
				return ( left.longValue() > right.longValue() );
			} else {
				return ( left.intValue() > right.intValue() );
			}
		}
	}, MINOR_OR_EQUAL {
		public final boolean evaluate( Value left, Value right ) {
			if ( left.isDouble() ) {
				return ( left.doubleValue() <= right.doubleValue() );
			} if ( left.isLong() ) {
				return ( left.longValue() <= right.longValue() );
			} else {
				return ( left.intValue() <= right.intValue() );
			}
		}
	}, MAJOR_OR_EQUAL {
		public final boolean evaluate( Value left, Value right ) {
			if ( left.isDouble() ) {
				return ( left.doubleValue() >= right.doubleValue() );
			} if ( left.isLong() ) {
				return ( left.longValue() >= right.longValue() );
			} else {
				return ( left.intValue() >= right.intValue() );
			}
		}
	};

	public abstract boolean evaluate( Value left, Value right );
}
