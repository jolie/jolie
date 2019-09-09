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

package jolie.lang.parse.util;

import java.net.URI;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.types.TypeDefinition;

/**
 * A {@code ProgramInspector} offers methods for accessing the data structures
 * of a {@link jolie.lang.parse.ast.Program} easily.
 * For instance, it is possible to obtain directly the list of interfaces
 * and ports defined in the referred JOLIE program.
 * @author Fabrizio Montesi
 */
public interface ProgramInspector
{
	/**
	 * Returns an array with all the sources parsed for generating the program.
	 * @return an array with all the sources parsed for generating the program
	 */
	public URI[] getSources();

	/**
	 * Returns an array of all the interfaces defined in the program.
	 * @return an array of all the interfaces defined in the program
	 */
	public InterfaceDefinition[] getInterfaces();

	/**
	 * Returns an array of all the input ports defined in the program.
	 * @return an array of all the input ports defined in the program
	 */
	public InputPortInfo[] getInputPorts();

	/**
	 * Returns an array of all the output ports defined in the program.
	 * @return an array of all the output ports defined in the program
	 */
	public OutputPortInfo[] getOutputPorts();

	/**
	 * Returns an array of all the types defined in the program.
	 * @return an array of all the types defined in the program
	 */
	public TypeDefinition[] getTypes();

	/**
	 * Returns an array of all the input ports defined in the specified source.
	 * @param source the target source
	 * @return an array of all the input ports defined in the specified source
	 */
	public InputPortInfo[] getInputPorts( URI source );

	/**
	 * Returns an array of all the output ports defined in the specified source.
	 * @param source the target source
	 * @return an array of all the output ports defined in the specified source
	 */
	public OutputPortInfo[] getOutputPorts( URI source );

	/**
	 * Returns an array of all the interfaces defined in the specified source.
	 * @param source the target source
	 * @return an array of all the interfaces defined in the specified source
	 */
	public InterfaceDefinition[] getInterfaces( URI source );

	/**
	 * Returns an array of all the types defined in the specified source.
	 * @param source the target source
	 * @return an array of all the types defined in the specified source
	 */
	public TypeDefinition[] getTypes( URI source );
        
        /**
	 * Returns an array of all the embedded service nodes in the specified source.
	 * @param source the target source
	 * @return an array of all the embedded service nodes defined in the specified source
	 */
	public EmbeddedServiceNode[] getEmbeddedServices( URI source );

	/**
	 * Returns an array of all the embedded service nodes defined in the program.
	 * @return an array of all the embedded service nodes defined in the program
	 */
	public EmbeddedServiceNode[] getEmbeddedServices( );
}
