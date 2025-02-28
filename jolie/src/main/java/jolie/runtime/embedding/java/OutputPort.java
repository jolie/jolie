/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.runtime.embedding.java;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import jolie.Interpreter;
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class OutputPort {
	private final Interpreter targetInterpreter;

	public OutputPort( Interpreter targetInterpreter ) {
		this.targetInterpreter = targetInterpreter;
	}

	public Interpreter targetInterpreter() {
		return targetInterpreter;
	}

	public CommChannel commChannel() {
		return this.targetInterpreter.commCore().getLocalCommChannel();
	}

	public void callOneWay( CommMessage message ) {
		try {
			CommChannel c = this.commChannel();

			c.send( message );
		} catch( IOException e ) {
			// This should never happen
			e.printStackTrace();
		}
	}

	public Value callRequestResponse( CommMessage request )
		throws FaultException {
		CommChannel c = this.commChannel();
		try {
			c.send( request );
			CommMessage response = c.recvResponseFor( request ).get();
			if( response.isFault() ) {
				throw response.fault();
			}
			return response.value();
		} catch( ExecutionException | InterruptedException | IOException e ) {
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e ); // TODO add context?
		}
	}

	public static OutputPort create( Interpreter interpreter, URI location ) {
		if( location == null ) {
			return new OutputPort( interpreter );
		}
		return new URIOutputPort( interpreter, location );
	}
}
