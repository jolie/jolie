/*
 * Copyright (C) 2017 Vincenzo Mattarella.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package jolie.tracer;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author Vincenzo Mattarella
 */
public class FaultTraceAction implements TraceAction {
     public static enum Type {
        FAULT_INSTALL,
        FAULT_SCOPE,
        FAULT_THROW
    }
     private Type type;
    private String message;
    private long timestamp;
    private String timeValue;
    private String value;
    private String sessionId;
    
    public FaultTraceAction(String sessionId, Type type, String message, String value,  long timestamp){
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
        this.timeValue = parsedTimestamp();
        this.value = value;
        this.sessionId = sessionId;
    }
    
    public String sessionId(){
        return sessionId;
    }
    public Type type(){
        return type;
    }
    
    public String message(){
        return message;
    }
    
    public String timeValue(){
        return timeValue;
    }
    public String value(){
        return value;
    }
      private String parsedTimestamp() {
            Date currentDate = new Date(timestamp);
            SimpleDateFormat dateFormatter = new SimpleDateFormat();
            dateFormatter.applyPattern("dd/MM/yy_HH.mm.ss.SSS");
            return dateFormatter.format(currentDate);
        }
}
