/***************************************************************************
 *   Copyright (C) 2008 by Roberto La Maestra                              *
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

package jolie.net.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLEngineResult.Status;

/**
 *
 * @author Roberto La Maestra
 * @author Fabrizio Montesi
 */
public class SSLInputStream extends InputStream
{
	private ByteArrayInputStream bufferInputStream;
	private final SSLProtocol sslProtocol;
	private final InputStream istream;
	private final OutputStream ostream;

	public SSLInputStream( byte[] buf, SSLProtocol sslProtocol, InputStream istream, OutputStream ostream )
	{
		this.bufferInputStream = new ByteArrayInputStream( buf );
		this.sslProtocol = sslProtocol;
		this.istream = istream;
		this.ostream = ostream;
	}

	@Override
	public int read()
		throws IOException
	{
		int b;
		byte[] ret;
		while ( (b = bufferInputStream.read()) == -1 ) {
			if ( b == -1 && sslProtocol.lastSSLStatus != Status.CLOSED ) {
				ret = sslProtocol.recvAux( istream, ostream );
				if ( ret == null ) {
					return -1;
				} else {
					bufferInputStream = new ByteArrayInputStream( ret );
				}
			} else {
				return -1;
			}
		}

		return b;
	}

	@Override
	public int read( byte[] b, int off, int len )
		throws IOException
	{
		int l = 0;
		byte[] ret;
		while ( (l = bufferInputStream.read( b, off, len )) == -1 ) {
			if ( l == -1 && sslProtocol.lastSSLStatus != Status.CLOSED ) {
				ret = sslProtocol.recvAux( istream, ostream );
				if ( ret == null ) {
					return -1;
				} else {
					bufferInputStream = new ByteArrayInputStream( ret );
				}
			} else {
				return -1;
			}
		}
		return l;
	}
}
