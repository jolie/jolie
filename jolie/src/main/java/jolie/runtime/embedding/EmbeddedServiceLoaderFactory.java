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

package jolie.runtime.embedding;

import jolie.Interpreter;
import jolie.runtime.expression.Expression;

/**
 * The factory interface for embedded service loader extensions.
 *
 * @author Fabrizio Montesi
 */
public interface EmbeddedServiceLoaderFactory {
	/**
	 * Create an {@link EmbeddedServiceLoader}.
	 *
	 * @param interpreter the embedding {@link Interpreter}
	 * @param configuration the {@link EmbeddedServiceLoader.EmbeddedServiceConfiguration}
	 * @param channelDest the intended destination for the channel towards the embedded service
	 * @return
	 * @throws EmbeddedServiceLoaderCreationException
	 */

	EmbeddedServiceLoader createLoader( Interpreter interpreter,
		EmbeddedServiceLoader.EmbeddedServiceConfiguration configuration,
		Expression channelDest )
		throws EmbeddedServiceLoaderCreationException;
}
