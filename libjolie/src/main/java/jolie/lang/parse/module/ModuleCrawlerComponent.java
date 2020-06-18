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

import java.nio.file.Path;

/**
 * A class consists of required components for dependencies crawling process in Jolie module system
 */
public class ModuleCrawlerComponent {

	private Finder finder;
	private ModuleParser parser;

	public ModuleCrawlerComponent( Path workingDirectory, String[] includePaths, String[] packagePaths,
		String charset, ClassLoader classLoader,
		boolean includeDocumentation ) {
		this( new FinderImpl( workingDirectory, packagePaths ),
			new ModuleParser( charset, includePaths, classLoader, includeDocumentation ) );
	}

	public ModuleCrawlerComponent( String[] packagePaths,
		ModuleParser parser ) {
		this( new FinderImpl( packagePaths ),
			parser );
	}

	public ModuleCrawlerComponent( Path workingDirectory, String[] packagePaths,
		ModuleParser parser ) {
		this( new FinderImpl( workingDirectory, packagePaths ),
			parser );
	}

	public ModuleCrawlerComponent( Finder finder, ModuleParser parser ) {
		this.finder = finder;
		this.parser = parser;
	}

	public Finder finder() {
		return finder;
	}

	public ModuleParser parser() {
		return parser;
	}

}
