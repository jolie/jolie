/*
 * Copyright (C) 2020 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class CodeCheckingException extends Exception {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final Collection< CodeCheckingError > errors;

	public CodeCheckingException( Collection< CodeCheckingError > errors ) {
		super(
			errors.stream().map( CodeCheckingError::toString ).collect( Collectors.joining( "\n" ) ) );
		this.errors = Collections.unmodifiableCollection( errors );
	}

	public Collection< CodeCheckingError > errors() {
		return errors;
	}
}
