/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import jolie.lang.Constants;

/**
 * Exception thrown when an invalid identifier is encountered.
 */
public class InvalidIdException extends Exception {
	private static final long serialVersionUID = Constants.serialVersionUID();

	/**
	 * Constructs a new InvalidIdException with the specified invalid identifier.
	 *
	 * @param id the invalid identifier
	 */
	public InvalidIdException( String id ) {
		super( "Invalid identifier: " + id );
	}

	/**
	 * Fills in the execution stack trace. Since this exception is used for control flow,
	 * filling in the stack trace is unnecessary and this method returns a reference to
	 * this exception.
	 *
	 * @return a reference to this exception
	 */
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
