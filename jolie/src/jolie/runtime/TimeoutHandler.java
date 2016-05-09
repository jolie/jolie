/***************************************************************************
 *   Copyright (C) 2009-2016 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.lang.ref.WeakReference;

/**
 * @author Fabrizio Montesi
 */
public abstract class TimeoutHandler implements Runnable
{
	private final long time;
	private volatile boolean cancelled = false;

	public TimeoutHandler( long timeout )
	{
		this.time = System.currentTimeMillis() + timeout;
	}

	public long time()
	{
		return time;
	}
	
	public void cancel()
	{
		cancelled = true;
	}
	
	public void run()
	{
		if ( !cancelled ) {
			onTimeout();
		}
	}

	protected abstract void onTimeout();

	/**
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	public static class Comparator implements java.util.Comparator< WeakReference< TimeoutHandler > >
	{
		@Override
		public int compare( WeakReference< TimeoutHandler > wt1, WeakReference< TimeoutHandler > wt2 )
		{
			TimeoutHandler t1 = ( wt1 != null ) ? wt1.get() : null;
			TimeoutHandler t2 = ( wt2 != null ) ? wt2.get() : null;
			if ( t1 == null ) {
				return -1;
			} else if ( t2 == null ) {
				return 1;
			}
			
			// Both are not null after this

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
