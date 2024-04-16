/***************************************************************************
 *   Copyright (C) 2006-2010 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.net;

import java.io.IOException;
import java.net.URI;

import javax.wsdl.WSDLException;

import jakarta.xml.soap.SOAPException;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.AndJarDeps;
import jolie.runtime.VariablePath;

@AndJarDeps( {
	"relaxngDatatype.jar",
	"xsom.jar",
	"wsdl4j.jar",
	"automaton.jar",
	"hamcrest-core.jar",
	"isorelax.jar",
	"jakarta.activation.jar",
	"jakarta.xml.soap-api.jar",
	"javax.xml.soap-api.jar",
	"jing.jar",
	"json-simple.jar",
	"Saxon-HE.jar",
	"stax-ex.jar",
	"webservices-rt.jar",
	"webservices-api.jar",
	"xercesImpl.jar"
} )
public class SoapProtocolFactory extends CommProtocolFactory {
	public SoapProtocolFactory( CommCore commCore ) {
		super( commCore );
	}

	@Override
	public CommProtocol createInputProtocol( VariablePath configurationPath, URI location )
		throws IOException {
		try {
			return new SoapProtocol( configurationPath, location, true, commCore().interpreter() );
		} catch( SOAPException | WSDLException e ) {
			throw new IOException( e );
		}
	}

	@Override
	public CommProtocol createOutputProtocol( VariablePath configurationPath, URI location )
		throws IOException {
		try {
			return new SoapProtocol( configurationPath, location, false, commCore().interpreter() );
		} catch( SOAPException | WSDLException e ) {
			throw new IOException( e );
		}
	}
}
