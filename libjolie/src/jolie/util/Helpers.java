/***************************************************************************
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.util;

import java.io.IOException;

/**
 * A convenience class with some helper functions for cleaner coding.
 * @author Fabrizio Montesi
 */
public class Helpers
{
	public final static long serialVersionUID = jolie.lang.Constants.serialVersionUID();
	
	public static <B extends Comparable<Boolean>, T extends Throwable> void condThrow( B condition, T throwable )
		throws T
	{
		if ( condition.compareTo( true ) == 0 ) {
			throw throwable;
		}
	}
	
	public static <B extends Comparable<Boolean>> void condThrowIOException( B condition, String message )
		throws IOException
	{
		if ( condition.compareTo( true ) == 0 ) {
			throw new IOException( message );
		}
	}
}
