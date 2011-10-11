/***************************************************************************
 *   Copyright (C) by Claudio Guidi <cguidi@italianasoftware.com>               *
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


import jolie.runtime.JavaService;
import jolie.runtime.Value;
import org.apache.commons.lang3.StringEscapeUtils;
import jolie.runtime.AndJarDeps;

@AndJarDeps({"commons-lang3-3.0.1.jar"})
public class HTMLUtils extends JavaService
{

	
	public Value unescapeHTML( Value request )
	{
		Value ret = Value.create();
		ret.setValue( StringEscapeUtils.unescapeHtml4( request.strValue() ));
		return ret;
	}

	
}
