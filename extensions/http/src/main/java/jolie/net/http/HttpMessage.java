/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.net.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class HttpMessage {
	public enum Type {
		RESPONSE, GET, HEAD, POST, DELETE, PUT, OPTIONS, PATCH, UNSUPPORTED, ERROR
	}

	public enum Version {
		HTTP_1_0, HTTP_1_1
	}

	static public class Cookie {
		private final String name, value, domain, path, expirationDate;
		private final boolean secure;

		public Cookie(
			String name,
			String value,
			String domain,
			String path,
			String expirationDate,
			boolean secure ) {
			this.name = name;
			this.value = value;
			this.domain = domain;
			this.path = path;
			this.expirationDate = expirationDate;
			this.secure = secure;
		}

		@Override
		public String toString() {
			return (name + "=" + value + "; " +
				"expires=" + expirationDate + "; " +
				"domain=" + domain + "; " +
				"path=" + path +
				((secure) ? ("; secure") : ""));
		}

		public String name() {
			return name;
		}

		public String value() {
			return value;
		}

		public String path() {
			return path;
		}

		public String domain() {
			return domain;
		}

		public String expirationDate() {
			return expirationDate;
		}

		public boolean secure() {
			return secure;
		}
	}

	private Version version;
	private final Type type;
	private byte[] content = null;
	final private Map< String, String > propMap = new HashMap<>();
	final private List< Cookie > setCookies = new ArrayList<>();

	final private Map< String, String > cookies = new HashMap<>();

	private int statusCode;
	private String requestPath;
	private String reason;
	private String userAgent = null;

	public boolean isSupported() {
		return type != Type.UNSUPPORTED;
	}

	public boolean isGet() {
		return type == Type.GET;
	}

	public boolean isHead() {
		return type == Type.HEAD;
	}

	public boolean isDelete() {
		return type == Type.DELETE;
	}

	public void addCookie( String name, String value ) {
		cookies.put( name, value );
	}

	public Map< String, String > cookies() {
		return cookies;
	}

	public void addSetCookie( Cookie cookie ) {
		setCookies.add( cookie );
	}

	@SuppressWarnings( "PMD" )
	public List< Cookie > setCookies() {
		return setCookies;
	}

	public HttpMessage( Type type ) {
		this.type = type;
	}

	protected void setVersion( Version version ) {
		this.version = version;
	}

	public Version version() {
		return version;
	}

	public void setContent( byte[] content ) {
		this.content = content;
	}

	public Collection< Entry< String, String > > properties() {
		return propMap.entrySet();
	}

	public void setRequestPath( String path ) {
		requestPath = path;
	}

	public void setUserAgent( String userAgent ) {
		this.userAgent = userAgent;
	}

	public void setProperty( String name, String value ) {
		propMap.put( name.toLowerCase(), value );
	}

	public String getProperty( String name ) {
		return propMap.get( name.toLowerCase() );
	}

	public String getPropertyOrEmptyString( String name ) {
		String ret = propMap.get( name.toLowerCase() );
		return (ret == null) ? "" : ret;
	}

	public String reason() {
		return reason;
	}

	public void setReason( String reason ) {
		this.reason = reason;
	}

	public int size() {
		if( content == null )
			return 0;
		return content.length;
	}

	public String requestPath() {
		return requestPath;
	}

	public String userAgent() {
		return userAgent;
	}

	public Type type() {
		return type;
	}

	public boolean isResponse() {
		return type == Type.RESPONSE;
	}

	public boolean isError() {
		return type == Type.ERROR;
	}

	public int statusCode() {
		return statusCode;
	}

	public void setStatusCode( int code ) {
		statusCode = code;
	}

	public String getMethod() {
		String method = "";
		if( type == Type.GET ||
			type == Type.POST ||
			type == Type.DELETE ||
			type == Type.PUT ||
			type == Type.PATCH ) {
			method = type.name();
		}
		return method;
	}

	public byte[] content() {
		return content;
	}
}
