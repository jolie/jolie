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

package jolie.lang.parse.module;

import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

/**
 * An implementation of Source for internal Jolie, which can be initiate directly from InputStream
 */
class InputStreamSource implements ModuleSource {

	private final InputStream is;
	private final URI uri;

	protected InputStreamSource( InputStream is, URI uri ) {
		this.is = is;
		this.uri = uri;
	}

	@Override
	public URI uri() {
		return this.uri;
	}

	/**
	 * the include path of ol file should be empty
	 */
	@Override
	public Optional< String > includePath() {
		return Optional.empty();
	}

	@Override
	public Optional< InputStream > openStream() {
		return Optional.of( this.is );
	}
}
