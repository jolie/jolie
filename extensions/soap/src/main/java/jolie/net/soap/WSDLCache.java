/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.net.soap;

import java.util.HashMap;
import java.util.Map;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

/**
 * Implements a WSDL document cache for the SOAP protocol.
 *
 * @author Fabrizio Montesi TODO: WSDL document removal after some time
 */
public class WSDLCache {
	// private static WSDLCache instance = null;

	private static final Map< String, Definition > CACHE = new HashMap<>();;
	private static WSDLFactory factory;

	static {
		try {
			factory = WSDLFactory.newInstance();
		} catch( WSDLException e ) {
			e.printStackTrace();
		}
	}

	public WSDLCache() {}


	public static synchronized Definition get( String url )
		throws WSDLException {
		Definition definition = CACHE.get( url );
		if( definition == null ) {
			WSDLReader reader = factory.newWSDLReader();
			reader.setFeature( "javax.wsdl.verbose", false );
			definition = reader.readWSDL( url );
			CACHE.put( url, definition );
		}
		return definition;
	}
}
