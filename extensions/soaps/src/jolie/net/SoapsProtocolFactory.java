/***************************************************************************
 *   Copyright (C) 2018 by Claudio Guidi <cguidi@italianasoftware.com>     *
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.protocols.CommProtocol;
import jolie.net.ssl.SSLProtocol;
import jolie.runtime.AndJarDeps;
import jolie.runtime.VariablePath;

@AndJarDeps({"jolie-ssl.jar"})
public class SoapsProtocolFactory extends CommProtocolFactory
{
	public SoapsProtocolFactory( CommCore commCore )
		throws ParserConfigurationException, TransformerConfigurationException
	{
		super( commCore );
	}

	public CommProtocol createOutputProtocol( VariablePath configurationPath, URI location )
		throws IOException
	{
		return new SSLProtocol(
			configurationPath,
			location,
			commCore().createOutputCommProtocol( "soap", configurationPath, location ),
			true
		);
	}

	public CommProtocol createInputProtocol( VariablePath configurationPath, URI location )
		throws IOException
	{
		return new SSLProtocol(
			configurationPath,
			location,
			commCore().createInputCommProtocol( "soap", configurationPath, location ),
			false
		);
	}
}
