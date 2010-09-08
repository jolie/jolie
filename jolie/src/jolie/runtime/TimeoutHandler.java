/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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
 * @author Fabrizio Montesi
 */
public abstract class TimeoutHandler
{
	private final long time;

	public TimeoutHandler( long timeout )
	{
		this.time = System.currentTimeMillis() + timeout;
	}

	public long time()
	{
		return time;
	}

	public abstract void onTimeout();

	/**
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	public static class Comparator implements java.util.Comparator< TimeoutHandler >
	{
		public int compare( TimeoutHandler t1, TimeoutHandler t2 )
		{
			if ( t1.time < t2.time ) {
				return -1;
			} else if ( t1.time == t2.time ) {
				return 0;
			} else {
				return 1;
			}
		}
	}
}
