package joliex.java;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

public class Jolie2JavaCommandLineParser extends CommandLineParser
{
	private String format = null;

	public String GetFormat()
		throws formatExeption
	{
		if ( format == null ) {
			throw new formatExeption();
		}

		return format;
	}

	private static class Jolie2JavaArgumentHandler implements CommandLineParser.ArgumentHandler
	{
		private String format = null;
		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException
		{
			if ( "--format".equals( argumentsList.get( index ) ) ) {
				index++;
				format = argumentsList.get( index );
			} else {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}

			return index;
		}
	}

	public static Jolie2JavaCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException
	{
		return new Jolie2JavaCommandLineParser( args, parentClassLoader, new Jolie2JavaArgumentHandler() );
	}

	private Jolie2JavaCommandLineParser( String[] args, ClassLoader parentClassLoader, Jolie2JavaArgumentHandler argHandler )
		throws CommandLineException, IOException
	{
		super( args, parentClassLoader, argHandler );
		format = argHandler.format;
	}
}