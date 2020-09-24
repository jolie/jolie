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
import jolie.Interpreter;
import jolie.net.CommChannel;

public class URIOutputPort extends OutputPort {
	private final URI location;

	public URIOutputPort( Interpreter targetInterpreter, URI location ) {
		super( targetInterpreter );
		this.location = location;
	}

	@Override
	public CommChannel commChannel() {
		try {
			CommChannel commChannel = super.targetInterpreter().commCore().createCommChannel( location, null );
			return commChannel;
		} catch( IOException e ) {
			// this should never happen, location is validated at service creation
			e.printStackTrace();
			return null;
		}
	}
}
