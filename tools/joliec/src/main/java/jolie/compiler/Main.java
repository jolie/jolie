/***************************************************************************
 *   Copyright (C) 2008-2014 by Fabrizio Montesi                           *
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

package jolie.compiler;

import java.io.IOException;

import jolie.cli.CommandLineException;
import jolie.lang.CodeCheckException;

/**
 *
 * @author Fabrizio Montesi
 */
public class Main {
	public static void main( String[] args ) {
		try {
			Compiler compiler = new Compiler( args );
			compiler.compile();
		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException | CodeCheckException e ) {
			e.printStackTrace();
		}
	}
}
