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

package joliex.wsdl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

/**
 *
 * @author Fabrizio Montesi
 */
public class WSDL2Jolie
{
	private static class CommandLineException extends Exception
	{
		public CommandLineException( String mesg )
		{
			super( mesg );
		}

		public CommandLineException()
		{
			super();
		}
	}

	public static void main( String[] args )
	{
		try {
			if ( args.length != 1 ) {
				throw new CommandLineException();
			}
			WSDLFactory factory = WSDLFactory.newInstance();
			WSDLReader reader = factory.newWSDLReader();

			Definition definition = reader.readWSDL( args[0] );
			WSDLConverter converter = new WSDLConverter( definition, new OutputStreamWriter( System.out ) );
			converter.convert();
		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
			System.out.println( "Syntax is wsdl2jolie <URL to WSDL>" );
		} catch( WSDLException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		}
	}
}
