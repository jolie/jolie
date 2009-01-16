/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

package jolie.doc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;

public class JolieDoc
{
    public static void main( String[] args )
	{
		if ( args.length < 1 ) {
			return;
		}
		try {
			InputStream olStream = new FileInputStream( args[0] );
			OLParser olParser = new OLParser(
					new Scanner( olStream, args[0] ),
					new String[] { "." },
					Thread.currentThread().getContextClassLoader()
				);
			Program program = olParser.parse();
			program = (new OLParseTreeOptimizer( program )).optimize();
			if ( !(new SemanticVerifier( program )).validate() ) {
				throw new IOException( "Exiting" );
			}
			JolieDocVisitor docVisitor = new JolieDocVisitor( program );
			docVisitor.visit( program );
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			e.printStackTrace();
		}
    }
}
