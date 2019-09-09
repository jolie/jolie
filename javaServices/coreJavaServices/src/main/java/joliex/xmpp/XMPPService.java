/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package joliex.xmpp;

import java.util.HashMap;
import java.util.Map;
import jolie.runtime.AndJarDeps;
import jolie.runtime.FaultException;
import jolie.runtime.Identifier;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.embedding.RequestResponse;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 *
 * @author Fabrizio Montesi
 */
@AndJarDeps({"smack.jar", "smackx.jar"})
public class XMPPService extends JavaService
{
	private XMPPConnection connection = null;

	private final Map< String, Chat > chats = new HashMap< String, Chat >();
	
	private Chat getChat( String userJID )
	{
		Chat chat = chats.get( userJID );
		if ( chat == null ) {
			chat = connection.getChatManager().createChat(
				userJID,
				new MessageListener() {
					public void processMessage( Chat chat, Message message )
					{
						// TODO redirect to embedder
					}
				}
			);
			chats.put( userJID, chat );
		}

		return chat;
	}

	@Identifier("sendMessage")
	@RequestResponse
	public void _sendMessage( Value request )
		throws FaultException
	{
		Chat chat = getChat( request.getFirstChild( "to" ).strValue() );
		try {
			chat.sendMessage( request.strValue() );
		} catch( XMPPException e ) {
			throw new FaultException( e );
		}
	}

	@RequestResponse
	public void connect( Value request )
		throws FaultException
	{
		if ( connection != null ) {
			connection.disconnect();
		}
		
		ConnectionConfiguration config;

		int port = request.getFirstChild( "port" ).intValue();
		if ( request.hasChildren( "host" ) && port > 0 ) {
			config = new ConnectionConfiguration(
				request.getFirstChild( "host" ).strValue(),
				port,
				request.getFirstChild( "serviceName" ).strValue()
			);
		} else {
			config = new ConnectionConfiguration(
				request.getFirstChild( "serviceName" ).strValue()
			);
		}

		connection = new XMPPConnection( config );
		try {
			connection.connect();
			if ( request.hasChildren( "resource" ) ) {
				connection.login(
					request.getFirstChild( "username" ).strValue(),
					request.getFirstChild( "password" ).strValue(),
					request.getFirstChild( "resource" ).strValue()
				);
			} else {
				connection.login(
					request.getFirstChild( "username" ).strValue(),
					request.getFirstChild( "password" ).strValue(),
					"Jolie"
				);
			}
		} catch( XMPPException e ) {
			throw new FaultException( "XMPPException", e );
		}
	}
}
