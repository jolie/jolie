/***************************************************************************
 *   Copyright 2014 (C) by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Fabrizio Montesi
 */
public class Installer {

	static final String windows = "windows";
	static final String unix = "unix";
	private final static int BUFFER_SIZE = 1024;
	private final String os = Helpers.getOperatingSystemType().toString().toLowerCase();
		
	private static void exec( File dir, String... args )
		throws IOException, InterruptedException
	{
		ProcessBuilder builder = new ProcessBuilder( args );
		builder.directory( dir );
		Process p = builder.start();
		p.waitFor();
		if ( p.getErrorStream() != null ) {
			int len = p.getErrorStream().available();
			if ( len > 0 ) {
				char[] buffer = new char[ len ];
				BufferedReader reader = new BufferedReader( new InputStreamReader( p.getErrorStream() ) );
				reader.read( buffer, 0, len );
				System.out.println( new String( buffer ) );
			}
		}
		p.destroy();
	}
	
	private File createTmpDir()
		throws IOException
	{
		File tmp = File.createTempFile( "jolie_installer_tmp", "" );
//		File tmp = new File( System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "jolie_installer" );
		tmp.delete();
		tmp.mkdir();
		tmp.deleteOnExit();
		return tmp;
	}
	
	private void copyDistZip( File parentDir )
		throws IOException
	{
		InputStream is = JolieInstaller.class.getClassLoader().getResourceAsStream( "dist.zip" );
		File distTmp = new File( parentDir, "dist.zip" );
		distTmp.createNewFile();
		new FileOutputStream( distTmp ).getChannel().transferFrom( Channels.newChannel( is ), 0, Long.MAX_VALUE );
	}
	
	private void copyInstallerScript( File parentDir )
		throws IOException, InterruptedException
	{
		InputStream is = JolieInstaller.class.getClassLoader().getResourceAsStream( "installer.zip" );
		File distTmp = new File( parentDir, "installer.zip" );
		distTmp.createNewFile();
		new FileOutputStream( distTmp ).getChannel().transferFrom( Channels.newChannel( is ), 0, Long.MAX_VALUE );
	}

	private File createTmpDist()
		throws IOException, InterruptedException, Exception
	{
		File tmp = createTmpDir();
		copyDistZip( tmp );
		if ( os.equals("windows")) { unzip( tmp.getAbsolutePath(), "dist.zip" ); } 
			else { exec( tmp, "unzip", "dist.zip" ); }
		
		copyInstallerScript( tmp );

		if ( os.equals("windows")){
			unzip( tmp.getAbsolutePath(), "installer.zip" );
		} else { exec( tmp, "unzip", "installer.zip" ); }	
		
		return new File( tmp, "dist" );
	}
	
//	private String getLauncher( File lsf ) throws IOException, FileNotFoundException {
//		int len;
//		char[] chr = new char[4096];
//		final StringBuffer buffer = new StringBuffer();
//		final FileReader reader = new FileReader( lsf );
//		try {
//			while ( ( len = reader.read( chr ) ) > 0 ) {
//				buffer.append(chr, 0, len);
//			}
//		} finally {
//			reader.close();
//		}
//		return buffer.toString();
//	}
	
//	private String getOSName( ClassLoader cl )
//		throws ClassNotFoundException, NoSuchMethodException,
//		IllegalAccessException, InvocationTargetException
//	{
//		Class<?> jolieClass = cl.loadClass( "jolie.util.Helpers" );
//		Method m = jolieClass.getMethod( "getOperatingSystemType" );
//		Object obj = m.invoke( null );
//		return obj.toString();
//	}
//	
	// MODIFY THIS METHOD TO READ FROM THE PROPER JOLIE LAUNCHER AND MODIFY IT
	// THE METHOD getLauncher ABOVE IS A STUB
	private String getLauncher( String os, String jolieHome ){
		if( os.equals( "windows" ) ){
			jolieHome = jolieHome + File.separator;
			return "java -ea:jolie... -ea:joliex... -Xmx1G -cp "
					+ jolieHome + "lib\\libjolie.jar;"
					+ jolieHome + "lib\\automaton.jar;"
					+ jolieHome + "lib\\jolie-js.jar;"
					+ jolieHome + "lib\\json_simple.jar;"
					+ jolieHome + "jolie.jar;"
					+ jolieHome + "jolie-cli.jar"
					+ " jolie.Jolie"
					+ " -l .\\lib\\*;"
					+ jolieHome + "lib;"
					+ jolieHome + "javaServices\\*;"
					+ jolieHome + "extensions\\*"
					+ " -i " + jolieHome + "\\include";
		} else {
			return "java -ea:jolie... -ea:joliex... -Xmx1G "
					+ "-Djava.rmi.server.codebase=file:/" + jolieHome + "/extensions/rmi.jar -cp "
					+ jolieHome + "/lib/libjolie.jar:"
					+ jolieHome + "/lib/automaton.jar:"
					+ jolieHome + "/lib/jolie-js.jar:"
					+ jolieHome + "/lib/json_simple.jar:"
					+ jolieHome + "/jolie.jar:"
					+ jolieHome + "/jolie-cli.jar"
					+ " jolie.Jolie"
					+ " -l ./lib/*:"
					+ jolieHome + "/lib:"
					+ jolieHome + "/javaServices/*:"
					+ jolieHome + "/extensions/*"
					+ " -i " + jolieHome + "/include";
		}
	}
	
	private int runCmd( String cmd ) throws InterruptedException {

	try {
		String line;
		final Process process = Runtime.getRuntime().exec( cmd );
		final InputStream stderr = process.getErrorStream();
		final InputStream stdin = process.getInputStream();
		final OutputStream stdout = process.getOutputStream();
		
		ExecutorService e1 = Executors.newSingleThreadExecutor();
		ExecutorService e2 = Executors.newSingleThreadExecutor();
		ExecutorService e3 = Executors.newSingleThreadExecutor();
		
		e1.execute( new Runnable() {
			@Override
			public void run() {
				try {	
					BufferedReader br = new BufferedReader( new InputStreamReader( System.in ));
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter( stdout ));

					String l;
					while( ( l = br.readLine() ) != null ){
						bw.write( l );
						bw.write( "\n" );
						bw.flush();
					}
					bw.close();
					br.close();
				} catch (IOException ex) {
//					Logger.getLogger(Installer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		
		e2.execute( new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader br = new BufferedReader( new InputStreamReader(stdin) );
					int c;
					while( ( c = br.read() ) != -1 ){
						System.out.print( (char) c );
					}
					br.close();
				} catch (IOException ex) {
//					Logger.getLogger(Installer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		
		e3.execute( new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader br = new BufferedReader( new InputStreamReader( stderr) );
					int c;
					while( ( c = br.read() ) != -1 ){
						System.out.print( (char) c );
					}
					br.close();
				} catch (IOException ex) {
//					Logger.getLogger(Installer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		int code = process.waitFor();
		e1.shutdown();
		e2.shutdown();
		e3.shutdown();
		process.destroy();
		return code;
	} catch (IOException err) {
//		err.printStackTrace();
		return 1;
		}
	}
	
	public  void unzip( String targetPath, String zipname ) throws Exception {
	byte[] buffer = new byte[ BUFFER_SIZE ];
	ZipInputStream zipInputStream;
	try {
		zipInputStream = new ZipInputStream(new FileInputStream( targetPath + File.separator + zipname ));				
		ZipEntry zipEntry = zipInputStream.getNextEntry();
		String filename;
		while( zipEntry != null ) {
			filename = zipEntry.getName();
			if ( !zipEntry.isDirectory() ) {
				File newFile = new File( targetPath + File.separator + filename );
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fileOutputStream = new FileOutputStream(newFile);             
				int len;
				while ((len = zipInputStream.read(buffer)) > 0) {
						fileOutputStream.write(buffer, 0, len);
				}
				fileOutputStream.close();   
			}
			zipEntry = zipInputStream.getNextEntry();				
    }
 
	zipInputStream.closeEntry();
   	zipInputStream.close();
				
	} catch( FileNotFoundException ex ) {
		throw new Exception("FileNotFound");
	} catch( IOException ex ) {
			throw new Exception("IOException");
	}
}

	/**
	 * Produces a whitespace separated argument string for parsing to the installer script
	 * @param args program arguments
	 * @return whitespace separated argument string or a empty string if no arguments
     */
	private String argumentBuilder( String[] args ) {
		if ( args != null && args.length > 0 ) {
			StringBuilder argsList = new StringBuilder();
			for ( int i = 0; i < args.length; ++i ) {
				argsList.append( args[i] );
				if ( i < args.length - 1 ) { // we don't like spaces at the end
					argsList.append( " " );
				}
			}
			return argsList.toString();
		}
		return "";
	}

	/**
	 * Validates the arguments parsed to the installer.
	 * Validation at this level is used to check whether argument to options fulfils certain formats
	 * ( such as path having no invalid characters in them etc. ).
	 * @throws IllegalArgumentException when a invalid argument is supplied
	 */
	private void validateArguments( String[] args ) throws IllegalArgumentException {
		if ( args != null && args.length > 0 ) {
			String path;
			for ( int i = 0; i < args.length; ++i ) {
				if ( ( args[ i ] == "-jh" || args[ i ] == "--jolie-home" || args[ i ] == "/jh" || args[ i ] == "/jolie-home" ) ||
				    ( args[ i ] == "-jl" || args[ i ] == "--jolie-launchers" || args[ i ] == "/jl" || args[ i ] == "/jolie-launchers" ) ) {
					if ( args[i + 1] != null ) {
						i++;
						path = args[i];
						try {
							Paths.get( path );
						} catch( InvalidPathException exception ) {
							throw new IllegalArgumentException( "Path contains invalid characters or is not a path" );
						}	
					}
				}
			}
		}
	}
	
	private int runJolie( String wdir, String jolieDir, String[] args ){
//		String ext = "";
//		String replaceVar;
		
//		if ( os.equals( "macos" ) || os.equals( "linux" ) ) {
//			os = "unix";
//		} 
//			else {
//			ext = ".bat";
//		}
		
		try {
			String arguments, cmd;
			cmd = getLauncher( os, jolieDir ); // get the corresponding jolie launcher script
			arguments = argumentBuilder( args ); // build argument string

			try {
				validateArguments( args );
			} catch ( IllegalArgumentException iae ) {
				System.err.println( "Error: Bad arguments: " + iae.getMessage() );
				System.exit(1);
				return 1;
			}

			cmd += " " + wdir + File.separator + "installer.ol " + os + " " + arguments;

			return runCmd( cmd );
			
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			return 2;
		}
	}
		
	public int run(String[] args)
		throws IOException, InterruptedException,
		ClassNotFoundException, NoSuchMethodException,
		IllegalAccessException, InvocationTargetException, Exception
	{
		File tmp = createTmpDist();
		String jolieDir = new File( tmp, "jolie" ).getAbsolutePath();
		char fs = File.separatorChar;

		return runJolie( tmp.getParent(), jolieDir, args );

//		URL[] urls = new URL[] { new URL( "file:" + jolieDir + fs + "jolie.jar" ), new URL( "file:" + jolieDir + fs + "lib" + fs + "libjolie.jar" ) };
//		ClassLoader cl = new URLClassLoader( urls, Installer.class.getClassLoader() );
//		Class<?> jolieClass = cl.loadClass( "jolie.Jolie" );
//		Method m = jolieClass.getMethod( "main", String[].class );
//		m.invoke(
//			null,
//			(Object) new String[] {
//			"-l",
//			jolieDir + fs + "lib" + fs + "*:"
//			+ jolieDir + fs + "lib:"
//			+ jolieDir + fs + "javaServices" + fs + "*:"
//			+ jolieDir + fs + "extensions" + fs + "*",
//			"-i",
//			jolieDir + fs + "include",
//			"--trace",
//			tmp.getParent() + fs + "installer.ol",
//			getOSName( cl ).toLowerCase()
//			}
//		);
	}
}
