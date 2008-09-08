package joliex.io;


import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ClosedByInterruptException;

import jolie.net.CommMessage;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class ConsoleService extends JavaService
{
	private class ConsoleInputThread extends Thread
	{
		private boolean keepRun = true;
		
		public void kill()
		{
			keepRun = false;
			this.interrupt();
		}

		@Override
		public void run()
		{
			BufferedReader stdin =
				new BufferedReader(
						new InputStreamReader(
							Channels.newInputStream(
								(new FileInputStream( FileDescriptor.in )).getChannel() ) ) );
			try {
				String line;
				while( keepRun ) {
					line = stdin.readLine();
					sendMessage( new CommMessage( "in", "/", Value.create( line ) ) );
				}
			} catch( ClosedByInterruptException ce ) {
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
	}
	
	private ConsoleInputThread consoleInputThread;
	
	public ConsoleService()
	{}
	
	public void registerForInput( CommMessage message )
	{
		consoleInputThread = new ConsoleInputThread();
		consoleInputThread.start();
	}
	
	@Override
	protected void finalize()
	{
		consoleInputThread.kill();
	}
	
	public void print( CommMessage message )
	{
		System.out.print( message.value().strValue() );
	}
	
	public void println( CommMessage message )
	{
		System.out.println( message.value().strValue() );
	}
}
