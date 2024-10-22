/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ByteArray {
	final private byte[] buffer;

	public ByteArray( byte[] buffer ) {
		this.buffer = buffer;
	}

	public int size() {
		return buffer.length;
	}

	public byte[] getBytes() {
		return buffer;
	}

	@Override
	public boolean equals( Object other ) {
		if( !(other instanceof ByteArray) )
			return false;
		return Arrays.equals( buffer, ((ByteArray) other).buffer );
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode( buffer );
	}

	@Override
	public String toString() {
		return new String( buffer );
	}

	public String toString( String charset ) throws UnsupportedEncodingException {
		return new String( buffer, charset );
	}
}
