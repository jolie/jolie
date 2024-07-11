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

package jolie.net;

import java.io.IOException;

/**
 * This interface is to be implemented by communication channels that are to be polled by
 * <code>CommCore</code>.
 *
 * @author Fabrizio Montesi
 * @see CommCore
 * @see CommChannel
 */
public interface PollableCommChannel {
	/**
	 * Returns <code>true</code> if this channel is ready for receiving an input message,
	 * <code>false</code> otherwise.
	 *
	 * @return <code>true</code> if this channel is ready for receiving an input message,
	 *         <code>false</code> otherwise
	 * @throws java.io.IOException
	 */
	boolean isReady()
		throws IOException;
}
