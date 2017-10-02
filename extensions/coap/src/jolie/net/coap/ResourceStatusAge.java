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
