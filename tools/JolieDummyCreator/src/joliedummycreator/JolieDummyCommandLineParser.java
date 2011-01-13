package joliedummycreator;

import java.io.IOException;
import java.util.List;
import jolie.CommandLineException;
import jolie.CommandLineParser;

public class JolieDummyCommandLineParser extends CommandLineParser
{
	private String nameOperation;

	public String getNameOperation()
		throws formatExeption
	{
		

		return nameOperation;
	}

	private static class JolieDummyArgumentHandler implements CommandLineParser.ArgumentHandler
	{
		private String nameOperation ;
		public int onUnrecognizedArgument( List< String > argumentsList, int index )
			throws CommandLineException
		{
			if ( "--output".equals( argumentsList.get( index ) ) ) {
				index++;

				nameOperation = argumentsList.get( index );
				index++;
			}
//			} else {
//				throw new CommandLineException( "Unrecognized command line option: " + argumentsList.get( index ) );
//			}

			return index;
		}
	}

	public static JolieDummyCommandLineParser create( String[] args, ClassLoader parentClassLoader )
		throws CommandLineException, IOException
	{
		return new JolieDummyCommandLineParser( args, parentClassLoader, new JolieDummyArgumentHandler() );
	}

	private JolieDummyCommandLineParser( String[] args, ClassLoader parentClassLoader, JolieDummyArgumentHandler argHandler )
		throws CommandLineException, IOException
	{
		super( args, parentClassLoader, argHandler );
		nameOperation = argHandler.nameOperation;
	}
}