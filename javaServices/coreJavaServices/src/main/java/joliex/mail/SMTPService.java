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
import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.RequestResponse;

@AndJarDeps( { "mailapi.jar", "smtp.jar" } )
public class SMTPService extends JavaService {
	private static class SimpleAuthenticator extends Authenticator {
		private final String username, password;

		public SimpleAuthenticator( String username, String password ) {
			this.username = username;
			this.password = password;
		}

		@Override
		public PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication( username, password );
		}
	}

	@RequestResponse
	public void sendMail( Value request )
		throws FaultException {
		/*
		 * Host & Authentication
		 */
		Authenticator authenticator = null;
		Properties props = new Properties();
		props.put( "mail.smtp.host", request.getFirstChild( "host" ).strValue() );
		if( request.hasChildren( "authenticate" ) ) {
			Value auth = request.getFirstChild( "authenticate" );
			props.put( "mail.smtp.auth", "true" );
			authenticator = new SimpleAuthenticator(
				auth.getFirstChild( "username" ).strValue(),
				auth.getFirstChild( "password" ).strValue() );
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
			if( request.hasChildren( "contentType" ) ) {
				type = request.getFirstChild( "contentType" ).strValue();
			}
			final String contentType = type;

			DataHandler dh = new DataHandler( new DataSource() {
				@Override
				public InputStream getInputStream()
					throws IOException {
					return new ByteArrayInputStream( contentText.getBytes() );
				}

				@Override
				public OutputStream getOutputStream()
					throws IOException {
					throw new IOException( "Operation not supported" );
				}

				@Override
				public String getContentType() {
					return contentType;
				}

				@Override
				public String getName() {
					return "mail attachment";
				}
			} );
			msg.setDataHandler( dh );

			if( request.hasChildren( "attachment" ) ) {
				MailcapCommandMap mailcapCommandMap = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
				mailcapCommandMap.addMailcap( "text/html;; x-java-content-handler=com.sun.mail.handlers.text_html" );
				mailcapCommandMap.addMailcap( "text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml" );
				mailcapCommandMap.addMailcap( "text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain" );
				mailcapCommandMap
					.addMailcap( "multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed" );
				mailcapCommandMap
					.addMailcap( "message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822" );
				Multipart multipart = new MimeMultipart();
				BodyPart messagePart = new MimeBodyPart();
				messagePart.setContent( request.getFirstChild( "content" ).strValue(), type );
				multipart.addBodyPart( messagePart );

				for( int counter = 0; counter < request.getChildren( "attachment" ).size(); counter++ ) {
					final String contentTypeMulti =
						request.getChildren( "attachment" ).get( counter ).getFirstChild( "contentType" ).strValue();
					final byte[] content = request.getChildren( "attachment" ).get( counter ).getFirstChild( "content" )
						.byteArrayValue().getBytes();
					dh = new DataHandler( new DataSource() {
						@Override
						public InputStream getInputStream()
							throws IOException {
							return new ByteArrayInputStream( content );
						}

						@Override
						public OutputStream getOutputStream()
							throws IOException {
							throw new IOException( "Operation not supported" );
						}

						@Override
						public String getContentType() {
							return contentTypeMulti;
						}

						@Override
						public String getName() {
							return "mail attachemt";
						}
					} );
					BodyPart attachmentPart = new MimeBodyPart();
					attachmentPart.setDataHandler( dh );
					attachmentPart.setFileName(
						request.getChildren( "attachment" ).get( counter ).getFirstChild( "filename" ).strValue() );
					multipart.addBodyPart( attachmentPart );
				}

				msg.setContent( multipart );

			}

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
