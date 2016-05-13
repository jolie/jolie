/***************************************************************************
 *   Copyright (C) 2010-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package joliex.util;

import java.io.IOException;
import java.io.StringWriter;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import joliex.wsdl.WSDLConverter;

@AndJarDeps({"wsdl4j.jar","jolie-xml.jar","xsom.jar","relaxngDatatype.jar"})
public class WebServicesUtils extends JavaService
{
	public String wsdlToJolie( String wsdlUrl )
		throws FaultException
	{
		StringWriter writer = new StringWriter();
		try {
			WSDLFactory factory = WSDLFactory.newInstance();
			WSDLReader reader = factory.newWSDLReader();

			Definition definition = reader.readWSDL( wsdlUrl );
			WSDLConverter converter = new WSDLConverter( definition, writer );
			converter.convert();
		} catch( WSDLException | IOException e ) {
			throw new FaultException( "IOException", e );
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return writer.toString();
	}
}
