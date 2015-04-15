/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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

package joliex.mail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

@AndJarDeps( {"mailapi.jar", "smtp.jar"} )
public class SMTPService extends JavaService
{
	private class SimpleAuthenticator extends Authenticator
	{
		private final String username, password;

		public SimpleAuthenticator( String username, String password )
		{
			this.username = username;
			this.password = password;
		}

		@Override
		public PasswordAuthentication getPasswordAuthentication()
		{
			return new PasswordAuthentication( username, password );
		}
	}

	@RequestResponse
	public void sendMail( Value request )
		throws FaultException
	{
		/*
		 * Host & Authentication
		 */
		Authenticator authenticator = null;
		Properties props = new Properties();
		props.put( "mail.smtp.host", request.getFirstChild( "host" ).strValue() );
		if ( request.hasChildren( "authenticate" ) ) {
			Value auth = request.getFirstChild( "authenticate" );
			props.put( "mail.smtp.auth", "true" );
			authenticator = new SimpleAuthenticator(
				auth.getFirstChild( "username" ).strValue(),
				auth.getFirstChild( "password" ).strValue()
			);
		}
		Session session = Session.getDefaultInstance( props, authenticator );
		Message msg = new MimeMessage( session );

		try {
			/*
			 * Recipents (To, Cc, Bcc)
			 */
			msg.setFrom( new InternetAddress( request.getFirstChild( "from" ).strValue() ) );
			for( Value v : request.getChildren( "to" ) ) {
				msg.addRecipient( Message.RecipientType.TO, new InternetAddress( v.strValue() ) );
			}
			for( Value v : request.getChildren( "cc" ) ) {
				msg.addRecipient( Message.RecipientType.CC, new InternetAddress( v.strValue() ) );
			}
			for( Value v : request.getChildren( "bcc" ) ) {
				msg.addRecipient( Message.RecipientType.BCC, new InternetAddress( v.strValue() ) );
			}

			/*
			 * Subject
			 */
			msg.setSubject( request.getFirstChild( "subject" ).strValue() );

			/*
			 * Content
			 */
			final String contentText = request.getFirstChild( "content" ).strValue();
			String type = "text/plain";
			if ( request.hasChildren( "contentType" ) ) {
				type = request.getFirstChild( "contentType" ).strValue();
			}
			final String contentType = type;
			DataHandler dh = new DataHandler( new DataSource()
			{
				public InputStream getInputStream()
					throws IOException
				{
					return new ByteArrayInputStream( contentText.getBytes() );
				}

				public OutputStream getOutputStream()
					throws IOException
				{
					throw new IOException( "Operation not supported" );
				}

				public String getContentType()
				{
					return contentType;
				}

				public String getName()
				{
					return "mail content";
				}
			} );
			msg.setDataHandler( dh );

			/*
			 * Reply To
			 */
			ValueVector replyTo = request.getChildren( "replyTo" );
			int nAddr = replyTo.size();
			Address[] replyAddr = new Address[ nAddr ];
			for( int i = 0; i < nAddr; i++ ) {
				replyAddr[ i ] = new InternetAddress( replyTo.get( i ).strValue() );
			}
			msg.setReplyTo( replyAddr );

			/*
			 * Send
			 */
			Transport.send( msg );
		} catch( MessagingException e ) {
			throw new FaultException( "SMTPFault", e );
		}
	}
}
