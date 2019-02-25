/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.email;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.email.support.JolieSmtpServer;

/**
 *
 * @author Balint Maschio
 */


public class JolieEmail
{
	

   public static void main(String[] args) {  
	   try {
		   ServerSocket serverSocket = new ServerSocket(25);
		   JolieSmtpServer jolieSmtpServer = new JolieSmtpServer(serverSocket);
	   } catch( IOException ex ) {
		   Logger.getLogger( JolieEmail.class.getName() ).log( Level.SEVERE, null, ex );
	   }
       
  
 }  
}

