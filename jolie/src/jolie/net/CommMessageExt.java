/********************************************************************************
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>  *
 *                                                                              *
 *   This program is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Library General Public License as            *
 *   published by the Free Software Foundation; either version 2 of the         *
 *   License, or (at your option) any later version.                            *
 *                                                                              *
 *   This program is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 *   GNU General Public License for more details.                               *
 *                                                                              *
 *   You should have received a copy of the GNU Library General Public          *
 *   License along with this program; if not, write to the                      *
 *   Free Software Foundation, Inc.,                                            *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                  *
 *                                                                              *
 *   For details about the authors of this software, see the AUTHORS file.      *
 ********************************************************************************/
package jolie.net;

import jolie.runtime.FaultException;
import jolie.runtime.Value;

public class CommMessageExt {

    private final CommMessage m;
    private boolean isRequest;

    public CommMessageExt(long id, String operationName, String resourcePath, Value value, FaultException fault) {
        this.m = new CommMessage(id, operationName, resourcePath, value, fault);
    }
    
    public CommMessageExt( CommMessage m ){
        this.m = m;
    }
    
    public CommMessageExt setRequest(){
        this.isRequest = true;
        return this; // returns "this" for easier composition e.g., new CommMessageExt( ... ).setRequest();
    }
    
    public CommMessage getCommMessage(){
        return m;
    }
    
    public boolean isRequest(){
        return this.isRequest;
    }

    public String resourcePath() {
        return m.resourcePath();
    }

    public boolean hasGenericId() {
        return m.hasGenericId();
    }

    public long id() {
        return m.id();
    }

    public static long getNewMessageId() {
        return CommMessage.getNewMessageId();
    }

    public static CommMessage createRequest(String operationName,
      String resourcePath, Value value) {
        return CommMessage.createRequest(operationName, resourcePath, value);
    }

    public static CommMessage createEmptyResponse(CommMessage request) {
        return CommMessage.createEmptyResponse(request);
    }

    public static CommMessage createResponse(CommMessage request, Value value) {
        return CommMessage.createResponse(request, value);
    }

    public static CommMessage createFaultResponse(CommMessage request, FaultException fault) {
        return CommMessage.createFaultResponse(request, fault);
    }

    public Value value() {
        return m.value();
    }

    ;
  public String operationName() {
        return m.operationName();
    }

    public boolean isFault() {
        return m.isFault();
    }

    public FaultException fault() {
        return m.fault();
    }
}