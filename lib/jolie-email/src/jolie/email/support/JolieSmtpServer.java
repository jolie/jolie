/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.email.support;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 *
 * @author maschio
 */
public class JolieSmtpServer implements AutoCloseable
{
	public static final int DEFAULT_SMTP_PORT = 25;

	/**
	 * pick any free port.
	 */
	public static final int AUTO_SMTP_PORT = 0;

	/**
	 * When stopping wait this long for any still ongoing transmission
	 */
	private static final int STOP_TIMEOUT = 20000;

	private static final Pattern CRLF = Pattern.compile( "\r\n" );

	/**
	 * Stores all of the email received since this instance started up.
	 */
	private final List<SmtpMessage> receivedMail;

	/**
	 * The server socket this server listens to.
	 */
	private final ServerSocket serverSocket;

	/**
	 * Thread that does the work.
	 */
	private final Thread workerThread;

	/**
	 * Indicates the server thread that it should stop
	 */
	private volatile boolean stopped = false;

	/**
	 * Creates an instance of a started SimpleSmtpServer.
	 *
	 * @param port port number the server should listen to
	 * @return a reference to the running SMTP server
	 * @throws IOException when listening on the socket causes one
	 */
	public static JolieSmtpServer start( int port ) throws IOException
	{
		return new JolieSmtpServer( new ServerSocket( Math.max( port, 0 ) ) );
	}

	public JolieSmtpServer( ServerSocket serverSocket )
	{
		this.receivedMail = new ArrayList<>();
		this.serverSocket = serverSocket;
		this.workerThread = new Thread(
			new Runnable()
		{
			@Override
			public void run()
			{
				performWork();
			}
		} );
		this.workerThread.start();
	}
    private void performWork() {
		try {
			// Server: loop until stopped
			while (!stopped) {
				// Start server socket and listen for client connections
				//noinspection resource
				try (Socket socket = serverSocket.accept();
				     Scanner input = new Scanner(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1)).useDelimiter(CRLF);
				     PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1));) {

					synchronized (receivedMail) {
						/*
						 * We synchronize over the handle method and the list update because the client call completes inside
						 * the handle method and we have to prevent the client from reading the list until we've updated it.
						 */
						
						receivedMail.addAll(handleTransaction(out, input));
					}
				}
			}
		} catch (Exception e) {
			// SocketException expected when stopping the server
			if (!stopped) {
				System.out.println("hit exception when running server");
				try {
					serverSocket.close();
				} catch (IOException ex) {
					System.out.println("and one when closing the port");
				}
			}
		}
	}
	@Override
	public void close() throws Exception
	{
		throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
	}
	
	private static List<SmtpMessage> handleTransaction(PrintWriter out, Iterator<String> input) throws IOException {
		// Initialize the state machine
		SmtpState smtpState = SmtpState.CONNECT;
		SmtpRequest smtpRequest = new SmtpRequest(SmtpActionType.CONNECT, "", smtpState);

		// Execute the connection request
		SmtpResponse smtpResponse = smtpRequest.execute();

		// Send initial response
		sendResponse(out, smtpResponse);
		smtpState = smtpResponse.getNextState();

		List<SmtpMessage> msgList = new ArrayList<>();
		SmtpMessage msg = new SmtpMessage();

		while (smtpState != SmtpState.CONNECT) {
			String line = input.next();
			
			if (line == null) {
				break;
			}

			// Create request from client input and current state
			SmtpRequest request = SmtpRequest.createRequest(line, smtpState);
			
			// Execute request and create response object
			SmtpResponse response = request.execute();
			// Move to next internal state
			smtpState = response.getNextState();
			System.out.println(smtpState.toString());
            // Send response to client
			sendResponse(out, response);

			// Store input in message
			String params = request.params;
			msg.store(response, params);

			// If message reception is complete save it
			if (smtpState == SmtpState.QUIT) {
				msgList.add(msg);
				msg = new SmtpMessage();
			}
		}

		return msgList;
	}
  	private static void sendResponse(PrintWriter out, SmtpResponse smtpResponse) {
		if (smtpResponse.getCode() > 0) {
			int code = smtpResponse.getCode();
			String message = smtpResponse.getMessage();
			out.print(code + " " + message + "\r\n");
			out.flush();
		}
	}
}
