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
import jolie.runtime.Value;

/**
 * 
 * @author Vincenzo Mattarella
 */
public class VariableTraceAction implements TraceAction{
    
    public static enum Type{
            ASSIGNMENT,
            PREINCREMENT,
            POSTINCREMENT,
            PREDECREMENT,
            POSTDECREMENT
    }
    
    private final Type type;
    private final String description;
    private final long timestamp;
    private final String timeValue;
    private final Value value;
    private final String origin;
    private final String variableName;
    private final String sessionId;
    
    public VariableTraceAction(Type type, String description, Value value, long timestamp){
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
        this.timeValue = parsedTimestamp();
        this.value = value;
        this.origin = "";
        this.variableName="";
        this.sessionId = "";
    }
     public VariableTraceAction(String sessionId, Type type,String description, String origin, String variableName, long timestamp){
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
        this.timeValue = parsedTimestamp();
        this.origin = origin;
        this.value = null;
        this.variableName = variableName;
        this.sessionId = sessionId;
    }
    
     public String sessionId(){
         return sessionId;
     }
    public Type type(){
        return type;
    }
    public String description(){
        return description;
}
    public String timeValue(){
        return timeValue;
    }
    
    public Value value(){
        return value;
    }
    
    public String origin(){
        return origin;
    }
    
    public String variableName(){
        return variableName;
    }
    
    private String parsedTimestamp() {
            Date currentDate = new Date(timestamp);
            SimpleDateFormat dateFormatter = new SimpleDateFormat();
            dateFormatter.applyPattern("dd/MM/yy_HH.mm.ss.SSS");
            return dateFormatter.format(currentDate);
        }
}
