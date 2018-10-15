/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi <famontesi@gmail.com>          *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as               *
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

/*
This code is a mess, but it works as an example and I'm too lazy to beautify it right now.
All the ad-hoc code should be put into an adapter and then used through embedding.
*/

include "console.iol"
include "message_digest.iol"
include "exec.iol"

constants {
	APIKey = "Your API key here",
	SharedSecret = "Your shared secret here",
	AuthenticatorURL = "http://www.rememberthemilk.com/services/auth/",
	Attrs = "@Attributes"
}

outputPort RTM {
Location: "socket://api.rememberthemilk.com:80/services/rest/"
Protocol:
	http {
		/*.debug = 1;
		.debugContent = 1;*/
		.aliases.echo = "?method=rtm.test.echo&api_key=%{api_key}";
		.aliases.getContactList = "?method=rtm.contacts.getList&api_key=%{api_key}&auth_token=%{auth_token}&api_sig=%{api_sig}";
		.aliases.getFrob = "?method=rtm.auth.getFrob&api_key=%{api_key}&api_sig=%{api_sig}";
		.aliases.getToken = "?method=rtm.auth.getToken&api_key=%{api_key}&frob=%{frob}&api_sig=%{api_sig}"
	}
RequestResponse:
	echo,
	getContactList,
	getFrob,
	getToken
}

define prepareRequest
{
	undef( request );
	request.api_key = APIKey
}

define prepareAuthedRequest
{
	prepareRequest;
	request.auth_token = auth_token
}

define authenticate
{
	println@Console( "Authenticating..." );
	prepareRequest;
	md5@MessageDigest( SharedSecret + "api_key" + APIKey + "method" + "rtm.auth.getFrob" )( request.api_sig );
	getFrob@RTM( request )( frobResponse );

	md5@MessageDigest(
		SharedSecret + "api_key" + APIKey + "frob" + frobResponse.frob + "perms" + "delete"
	)( sig );
	println@Console( "Waiting for Konqueror to be closed..." );
	exec@Exec(
		"konqueror http://www.rememberthemilk.com/services/auth/?api_key=" +
		APIKey + "&perms=delete&frob=" + frobResponse.frob + "&api_sig=" + sig
	)();

	prepareRequest;
	md5@MessageDigest(
		SharedSecret + "api_key" + APIKey + "frob" + frobResponse.frob + "method" + "rtm.auth.getToken"
	)( request.api_sig );
	request.frob = frobResponse.frob;
	getToken@RTM( request )( response );
	auth_token = response.auth.token
}

main
{
	authenticate;

	prepareAuthedRequest;
	md5@MessageDigest(
		SharedSecret + "api_key" + APIKey + "auth_token" + auth_token + "method" + "rtm.contacts.getList"
	)( request.api_sig );
	println@Console( "\nRetrieving contact list..." );
	getContactList@RTM( request )( response );
	println@Console( "Found " + #response.contacts.contact + " contacts:" );
	contact[0] -> response.contacts.contact[i];
	for( i = 0, i < #response.contacts.contact, i++ ) {
		println@Console( "\t" + contact.(Attrs).fullname + " (username: " + contact.(Attrs).username + ")" )
	}
}
