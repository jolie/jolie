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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import jolie.lang.Constants;

/**
 * Java representation for a Jolie fault.
 * 
 * @author Fabrizio Montesi
 */
public class FaultException extends Exception {
	private static final long serialVersionUID = jolie.lang.Constants.serialVersionUID();
	private final String faultName;
	private final Value value;

	/**
	 * Constructor. This constructor behaves as
	 * {@code FaultException( faultName, Value.create( t.getMessage() ) )}, but it also adds a
	 * {@code stackTrace} subnode to the value of this fault containing the stack trace of the passed
	 * {@link Throwable} t.
	 * 
	 * @param faultName the name of the fault
	 * @param t the {@link Throwable} whose message and stack trace should be read
	 */
	public FaultException( String faultName, Throwable t ) {
		this( faultName, Value.create( t.getMessage() ) );
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		t.printStackTrace( new PrintStream( bs ) );
		value.getNewChild( "stackTrace" ).setValue( bs.toString() );
	}

	/**
	 * Constructor. Shortcut for {@code FaultException( t.getClass().getSimpleName(), t )}.
	 * 
	 * @param t
	 */
	public FaultException( Throwable t ) {
		this( t.getClass().getSimpleName(), t );
	}

	/**
	 * Constructor. Shortcut for {@code FaultException( faultName, Value.create( message ) )}
	 * 
	 * @param faultName
	 * @param message
	 */
	public FaultException( String faultName, String message ) {
		this( faultName, Value.create( message ) );
	}

	/**
	 * Constructor.
	 * 
	 * @param faultName the name of the fault
	 * @param value the {@link Value} containing the fault data
	 */
	public FaultException( String faultName, Value value ) {
		super();
		this.faultName = faultName;
		this.value = value;
	}

	/**
	 * Constructor. Shortcut for {@code FaultException( faultName, Value.create() )}
	 * 
	 * @param faultName
	 */
	public FaultException( String faultName ) {
		this( faultName, Value.create() );
	}

	@Override
	public String getMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append( faultName );
		builder.append( ": " );
		builder.append( value.strValue() );
		return builder.toString();
	}

	/**
	 * Returns the {@link Value} of this fault.
	 * 
	 * @return the {@link Value} of this fault.
	 */
	public Value value() {
		return value;
	}

	/**
	 * Returns the name of this fault instance.
	 * 
	 * @return the name of this fault instance
	 */
	public String faultName() {
		return faultName;
	}

	public RuntimeFaultException toRuntimeFaultException() {
		return new RuntimeFaultException( this );
	}

	// A RuntimeFaultException is used for runtime errors from which it is
	// impossible to recover from and continue the execution.
	// A RuntimeFaultException wraps a FaultException.
	// A thrown RuntimeFaultException is caught by the enclosing execution
	// instance and used to report to the user the enclosed FaultException
	public static class RuntimeFaultException extends RuntimeException {
		private static final long serialVersionUID = Constants.serialVersionUID();

		private final FaultException faultException;

		private RuntimeFaultException( FaultException faultException ) {
			this.faultException = faultException;
		}

		public FaultException faultException() {
			return this.faultException;
		}
	}
}
