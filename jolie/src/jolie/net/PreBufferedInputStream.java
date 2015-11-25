/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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


package jolie.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class PreBufferedInputStream extends BufferedInputStream
{
	private final static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
	
	public PreBufferedInputStream( InputStream istream )
	{
		super( istream );
	}

	public boolean hasCachedData()
	{
		return pos < count;
	}

	public void append( ByteBuffer b )
	{
		final int bufferSize = b.remaining();
		enlargeIfNecessary( bufferSize );
		b.get( buf, count, bufferSize );
		count += bufferSize;
	}
	
	private void enlargeIfNecessary( int toBeWritten )
	{
		if ( count + toBeWritten >= buf.length ) {
			int tentative = Math.max( count * 2, count + toBeWritten );
			if ( tentative >= MAX_BUFFER_SIZE ) {
				if ( count + toBeWritten < MAX_BUFFER_SIZE ) {
					tentative = count + toBeWritten;
				} else {
					throw new OutOfMemoryError( "Required array size too large" );
				}
			}
			
			final byte nbuf[] = new byte[tentative];
			final int remaining = count - pos;
			System.arraycopy( buf, pos, nbuf, 0, remaining );
			buf = nbuf;
			pos = 0;
			count = remaining;
		}
	}
	
	public void append( byte b )
	{
		enlargeIfNecessary( 1 );
		buf[ count++ ] = b;
	}
}
