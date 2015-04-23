/***************************************************************************
 *   Copyright (C) by Claudio Guidi                                  *
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

package joliex.gwt.client;

import java.util.Arrays;

public class ByteArray
{
	final private byte[] buffer;
	
	public ByteArray( byte[] buffer )
	{
		this.buffer = buffer;
	}
	
	public int size()
	{
		return buffer.length;
	}
	
	public byte[] getBytes()
	{
		return buffer;
	}

	public boolean equals( ByteArray other )
	{
		return Arrays.equals( buffer, other.buffer );
	}
	
	@Override
	public String toString()
	{
		char[] chars = new char[ buffer.length / 2 ];  // 2 bytes for each char
		for( int i = 0; i < chars.length; i++ ) {
			for( int j = 0; j < 2; j++ ) {
				int shift = (1 - j) * 8;
				chars[ i ] |= (0x000000FF << shift) & (((int) buffer[ i * 2 + j ]) << shift);
			}
		}
		return new String( chars );
	}
}
