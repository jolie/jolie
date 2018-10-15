/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.util;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.UriTemplateMatcherFactory;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.runtime.AndJarDeps;
import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

@AndJarDeps({"uri-templates.jar"})
public class UriTemplates extends JavaService
{
	public Value match( Value request )
	{
		UriTemplate t = UriTemplate.fromTemplate( request.getFirstChild( "template" ).strValue() );
		Pattern p = UriTemplateMatcherFactory.getReverseMatchPattern( t );
		Matcher m = p.matcher( request.getFirstChild( "uri" ).strValue() );
		Value response = Value.create();
		boolean matches = m.matches();
		response.setValue( matches );
		if ( matches ) {
			for( String param : t.getVariables() ) {
				response.setFirstChild( param, m.group( param ) );
			}
		}
		return response;
	}
	
	public String expand( Value request )
	{
		UriTemplate t = UriTemplate.fromTemplate( request.getFirstChild( "template" ).strValue() );
		if ( request.hasChildren( "params" ) ) {
			for( final Map.Entry< String, ValueVector > entry : request.getFirstChild( "params" ).children().entrySet() ) {
				t.set( entry.getKey(), entry.getValue().first().valueObject() );
			}
		}
		return t.expand();
	}
}
