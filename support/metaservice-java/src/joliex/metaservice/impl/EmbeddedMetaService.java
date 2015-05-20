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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jolie.CommandLineException;
import jolie.Interpreter;
import jolie.net.CommChannel;
import joliex.metaservice.MetaService;
import joliex.metaservice.MetaServiceChannel;

/**
 * This class executes a MetaService service in the local Java Virtual Machine.
 * @author Fabrizio Montesi
 */
public class EmbeddedMetaService extends MetaService
{	
	private final Interpreter interpreter;
	private final MetaServiceChannel channel;
	private static final String fs = jolie.lang.Constants.fileSeparator;
	private static final String ps = jolie.lang.Constants.pathSeparator;

	private static final String JOLIE_HOME_ENV = "JOLIE_HOME";

	private static final String defaultFilepath = fs + "include" + fs + "services" + fs + "metaservice" + fs + "metaservice.ol";
	
	private static String[] buildInterpreterArguments( String jh, String metaServiceFilepath )
	{
		// jh stays for jolie_home, i.e. the JOLIE installation directory
		return new String[] {
			// Location
			"-C", "MetaServiceLocation=\"local\"",
			// Libraries
			"-l", jh + fs + "lib" + ps + jh + fs + "javaServices" + fs + "*" + ps + jh + fs + "extensions" + fs + "*",
			// Includes
			"-i", jh + fs + "include",
			// MetaService source file
			metaServiceFilepath
		};
	}
	
	/**
	 * Creates an embedded MetaService instance, 
	 * executing a JOLIE interpreter in the local JVM.
	 */
	public EmbeddedMetaService()
		throws IOException, ExecutionException
	{
		this( System.getenv( JOLIE_HOME_ENV ) );
	}

	/**
	 * Creates an embedded MetaService instance, 
	 * executing a JOLIE interpreter in the local JVM.
	 * @param jolieHome the path pointing to the local JOLIE installation directory.
	 */
	public EmbeddedMetaService( String jolieHome )
		throws IOException, ExecutionException
	{
		this( jolieHome, jolieHome + defaultFilepath );
	}
	
	private void startInterpreter()
		throws ExecutionException
	{
		Future< Exception > f = interpreter.start();
		try {
			Exception e = f.get();
			if ( e != null ) {
				throw new ExecutionException( e );
			}
		} catch( InterruptedException e ) {
			throw new ExecutionException( e );
		}
	}
	
	/**
	 * Creates an embedded MetaService instance, 
	 * executing a JOLIE interpreter in the local JVM.
	 * @param jolieHome the path pointing to the local JOLIE installation directory.
	 * @param metaserviceFilepath the path pointing to the metaservice source file to load.
	 */
	public EmbeddedMetaService( String jolieHome, String metaserviceFilepath )
		throws IOException, ExecutionException
	{
		try {
			interpreter =
				new Interpreter( buildInterpreterArguments( jolieHome, metaserviceFilepath ), this.getClass().getClassLoader(), null );
			startInterpreter();
			channel = new MetaServiceChannel( this, "/" );
		} catch( CommandLineException e ) {
			throw new IOException( e );
		} catch( FileNotFoundException e ) {
			throw new IOException( e );
		}
	}
	
	protected CommChannel createCommChannel()
	{
		return interpreter.commCore().getLocalCommChannel();
	}
	
	public MetaServiceChannel getChannel()
	{
		return channel;
	}
}
