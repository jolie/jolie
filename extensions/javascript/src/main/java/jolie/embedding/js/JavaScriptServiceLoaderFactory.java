/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.embedding.js;

import jolie.Interpreter;
import jolie.runtime.AndJarDeps;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.embedding.EmbeddedServiceLoaderFactory;
import jolie.runtime.expression.Expression;

/**
 * An embedding extension for JavaScript programs.
 * 
 * @author Fabrizio Montesi
 */
@AndJarDeps({"jolie-js.jar","json_simple.jar"})
public class JavaScriptServiceLoaderFactory implements EmbeddedServiceLoaderFactory
{
	@Override
	public EmbeddedServiceLoader createLoader( Interpreter interpreter, String type, String servicePath, Expression channelDest )
		throws EmbeddedServiceLoaderCreationException
	{
		return new JavaScriptServiceLoader( channelDest, servicePath );
	}
}
