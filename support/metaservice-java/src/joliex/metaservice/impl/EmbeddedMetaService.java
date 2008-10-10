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

package joliex.metaservice.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.LocalCommChannel;
import jolie.runtime.Value;
import joliex.metaservice.MetaService;

/**
 * This class executes a MetaService service in the local Java Virtual Machine.
 * @author Fabrizio Montesi
 */
public class EmbeddedMetaService extends MetaService
{	
	final private CommChannel commChannel;
	
	final private static String defaultFilepath = "metaservice.ol";
	
	private static String[] buildInterpreterArguments( String jh, String metaServiceFilepath )
	{
		// jh stays for jolie_home, i.e. the JOLIE installation directory
		
		final String fs = jolie.Constants.fileSeparator;
		final String ps = jolie.Constants.pathSeparator;
		
		return new String[] {
			// Libraries
			"-l", jh + fs + "lib" + ps + jh + "javaServices/*" + ps + jh + "extensions/*",
			// Includes
			"-i", jh + fs + "include",
			// MetaService source file
			metaServiceFilepath
		};
	}
	
	/**
	 * Creates an embedded MetaService instance, 
	 * executing a JOLIE interpreter in the local JVM.
	 * @param jolieHome the path pointing to the local JOLIE installation directory.
	 */
	public EmbeddedMetaService( String jolieHome )
		throws IOException
	{
		try {
			Interpreter interpreter =
				new Interpreter( buildInterpreterArguments( jolieHome, defaultFilepath ) );
			commChannel = new LocalCommChannel( interpreter );
		} catch( CommandLineException e ) {
			throw new IOException( e );
		} catch( FileNotFoundException e ) {
			throw new IOException( e );
		}
	}
	
	/**
	 * Creates an embedded MetaService instance, 
	 * executing a JOLIE interpreter in the local JVM.
	 * @param jolieHome the path pointing to the local JOLIE installation directory.
	 * @param metaserviceFilepath the path pointing to the metaservice source file to load.
	 */
	public EmbeddedMetaService( String jolieHome, String metaserviceFilepath )
		throws IOException
	{
		try {
			Interpreter interpreter =
				new Interpreter( buildInterpreterArguments( jolieHome, metaserviceFilepath ) );
			commChannel = new LocalCommChannel( interpreter );
		} catch( CommandLineException e ) {
			throw new IOException( e );
		} catch( FileNotFoundException e ) {
			throw new IOException( e );
		}
	}
	
	protected void sendMessage( String operationName, Value value )
		throws IOException
	{
		commChannel.send( new CommMessage( operationName, "/", value ) );
	}
	
	protected CommMessage recvMessage()
		throws IOException
	{
		return commChannel.recv();
	}
}
