package joliex.java;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import joliex.java.formatExeption;

public class Jolie2JavaCommandLineParser extends CommandLineParser
{
	private String namespace;
    private String format;
	
	public String getNameSpace()
		throws formatExeption
	{
		

		return namespace;
	}
	public String getFormat()
	{

	  return format;
	}

	private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler
	{
		private String namespace ;
		private String format;
                private String target;
		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException
		{
			if ( "--namespace".equals( argumentsList.get( index ) ) ) {
				index++;

				namespace = argumentsList.get( index );
				//index++;
			} else if ( "--format".equals( argumentsList.get( index ) ) ) {
				index++;
				format = argumentsList.get( index );
			} else if ( "--target".equals( argumentsList.get( index ) ) ) {
				index++;
				target = argumentsList.get( index );
			} else {
				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
			}
//			} else {
//				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
//			}

			return index;
		}
	}

	public static Jolie2JavaCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException
	{
		return new Jolie2JavaCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
	}

	private Jolie2JavaCommandLineParser( String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler )
		throws CommandLineException, IOException
	{
		super( args, parentClassLoader, argHandler );

		namespace = argHandler.namespace;
		format= argHandler.format;
	}
}