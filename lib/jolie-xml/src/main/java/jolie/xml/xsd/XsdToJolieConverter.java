/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.xml.xsd;

import java.util.List;

import jolie.lang.Constants;
import jolie.lang.parse.ast.types.TypeDefinition;

/**
 *
 * @author Fabrizio Montesi
 */
public interface XsdToJolieConverter {
	public static class ConversionException extends Exception {
		private static final long serialVersionUID = Constants.serialVersionUID();

		public ConversionException( String message ) {
			super( message );
		}
	}

	/**
	 * Converts a schema set into a list of JOLIE type definitions.
	 * 
	 * @return a list of JOLIE type definitions obtained by reading the passed schema set.
	 * @throws ConversionException if an unsupported XSD element is encountered
	 * @see TypeDefinition
	 */
	public List< TypeDefinition > convert()
		throws ConversionException;
}
