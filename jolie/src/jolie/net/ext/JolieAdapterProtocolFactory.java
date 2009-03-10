/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package jolie.net.ext;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.runtime.VariablePath;
import jolie.net.protocols.CommProtocol;
import jolie.net.CommCore;
import jolie.net.protocols.JolieAdapterProtocol;
import jolie.runtime.Value;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.embedding.JolieServiceLoader;

public class JolieAdapterProtocolFactory extends CommProtocolFactory
{
	final private CommChannel adapterChannel;
	final private Interpreter adaptor;

	public JolieAdapterProtocolFactory( CommCore commCore, String filepath, URL jarURL )
	{
		super( commCore );
		Value channelValue = Value.create();
		JolieServiceLoader loader = null;
		try {
			loader = new JolieServiceLoader(
				channelValue,
				commCore.interpreter(),
				filepath
			);
			loader.load();
		} catch( EmbeddedServiceLoadingException e ) {
			e.printStackTrace();
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( CommandLineException e ) {
			e.printStackTrace();
		}
		adaptor = ( loader == null ) ? null : loader.interpreter();
		adapterChannel = channelValue.channelValue();
	}

	public CommProtocol createProtocol( VariablePath configurationPath, URI location )
		throws IOException
	{
		if ( adaptor == null || adapterChannel == null ) {
			throw new IOException();
		}
		return new JolieAdapterProtocol( configurationPath, location, adapterChannel, adaptor );
	}
}
