/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net.coap;

import jolie.Interpreter;

public class ResourceStatusAge {

    public static final long MODULUS = (long) Math.pow(2, 24);
    private static final long THRESHOLD = (long) Math.pow(2, 23);
    private static String msg = "";
    private long sequenceNo;
    private long timestamp;

    public ResourceStatusAge(long sequenceNo, long timestamp) {
	this.sequenceNo = sequenceNo;
	this.timestamp = timestamp;
    }

    public static boolean isReceivedStatusNewer(ResourceStatusAge latest,
	    ResourceStatusAge received) {
	if (latest.sequenceNo < received.sequenceNo && received.sequenceNo
		- latest.sequenceNo < THRESHOLD) {
	    msg = "Criterion 1 matches: received (" + received + ") "
		    + "is newer than latest (" + latest + ").";
	    Interpreter.getInstance().logInfo(msg);
	    return true;
	}

	if (latest.sequenceNo > received.sequenceNo && latest.sequenceNo
		- received.sequenceNo > THRESHOLD) {
	    msg = "Criterion 2 matches: received (" + received + ") "
		    + "is newer than latest (" + latest + ").";
	    Interpreter.getInstance().logInfo(msg);
	    return true;
	}

	if (received.timestamp > latest.timestamp + 128000L) {
	    msg = "Criterion 3 matches: received (" + received + ") "
		    + "is newer than latest (" + latest + ").";
	    Interpreter.getInstance().logInfo(msg);
	    return true;
	}

	msg = "No Criterion matches: received (" + received + ") "
		+ "is newer than latest (" + latest + ").";
	Interpreter.getInstance().logInfo(msg);
	return false;
    }

    @Override
    public String toString() {
	return "STATUS AGE (Sequence No: " + this.sequenceNo
		+ ", Reception Timestamp: " + this.timestamp + ")";
    }
}
