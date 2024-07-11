/***************************************************************************
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

package jolie.net.http;

import java.io.IOException;
import jolie.lang.Constants;

/**
 * An exception for invalid/unsupported HTTP requests
 *
 * @author Matthias Dieter Wallnöfer
 */
public class UnsupportedMethodException extends IOException {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final Method[] allowedMethods;

	/**
	 * Constructor.
	 *
	 * @param message
	 */
	public UnsupportedMethodException( String message ) {
		this( message, (Method[]) null );
	}

	/**
	 * Constructor.
	 *
	 * @param message
	 * @param allowedMethods
	 */
	public UnsupportedMethodException( String message, Method... allowedMethods ) {
		super( message );
		this.allowedMethods = allowedMethods;
	}

	/**
	 * Returns the allowed methods if they have been specified
	 */
	public Method[] allowedMethods() {
		return allowedMethods;
	}
}
