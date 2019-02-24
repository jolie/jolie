/***************************************************************************
 *   Copyright (C) 2014 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie.tracer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Fabrizio Montesi
 */
public class EmbeddingTraceAction implements TraceAction
{
	public static enum Type {
		SERVICE_LOAD
	}
	
	private final Type type;
	private final String name;
	private final String description;
        private final long timestamp;
        private final String timeValue;
        private final String sessionId;
	
	public EmbeddingTraceAction( String sessionId, Type type, String name, String description, long timestamp )
	{
		this.type = type;
		this.name = name;
		this.description = description;
                this.timestamp = timestamp;
                this.timeValue = parsedTimestamp();
                this.sessionId = sessionId;
	}
        
        public String sessionId(){
            return sessionId;
        }
	
	public Type type()
	{
		return type;
	}
	
	public String name()
	{
		return name;
	}
	
	public String description()
	{
		return description;
	}
        
        public String timeValue(){
            return timeValue;
        }
        
        private String parsedTimestamp() {
            Date currentDate = new Date(timestamp);
            SimpleDateFormat dateFormatter = new SimpleDateFormat();
            dateFormatter.applyPattern("dd/MM/yy_HH.mm.ss.SSS");
            return dateFormatter.format(currentDate);
        }
}
