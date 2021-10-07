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

package jolie.lang.parse.module.exceptions;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jolie.lang.Constants;

public class ModuleNotFoundException extends FileNotFoundException {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final List< String > lookedPaths;
	private final String moduleName;

	public ModuleNotFoundException( String moduleName, List< String > lookedPaths ) {
		super( moduleName );
		this.moduleName = moduleName;
		this.lookedPaths = lookedPaths;
	}

	public ModuleNotFoundException( String moduleName, String lookedPath ) {
		super( moduleName );
		this.moduleName = moduleName;
		this.lookedPaths = new ArrayList<>();
		this.lookedPaths.add( lookedPath );
	}

	@Override
	public String getMessage() {
		return "Module \"" + this.moduleName + "\" not found from lookup path "
			+ Arrays.toString( this.lookedPaths.toArray() );
	}

}
