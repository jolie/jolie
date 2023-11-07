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

package jolie;

import jolie.lang.Constants;

/**
 * Thrown by Interpreter if the interpretation tree could not be built.
 *
 * @author Fabrizio Montesi
 */
public class InterpreterException extends Exception {
	final private static long serialVersionUID = Constants.serialVersionUID();

	/**
	 * Constructor
	 *
	 * @param message the message of this <code>InterpreterException</code>
	 */
	public InterpreterException( String message ) {
		super( message );
	}

	/**
	 * Constructor
	 *
	 * @param cause the <code>Throwable</code> that caused this <code>InterpreterException</code>
	 */
	public InterpreterException( Throwable cause ) {
		super( cause );
	}

	/**
	 * Constructor
	 *
	 * @param message the message of this <code>InterpreterException</code>
	 * @param cause the <code>Throwable</code> that caused this <code>InterpreterException</code>
	 */
	public InterpreterException( String message, Throwable cause ) {
		super( message, cause );
	}
}
