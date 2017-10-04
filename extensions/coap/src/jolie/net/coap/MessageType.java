package jolie.net.coap;

import java.util.HashMap;

public abstract class MessageType {

    public static final int CON = 0;
    public static final int NON = 1;
    public static final int ACK = 2;
    public static final int RST = 3;

    private static final HashMap<Integer, String> MESSAGE_TYPES
	    = new HashMap<>();

    static {
	MESSAGE_TYPES.put(CON, "CON (" + CON + ")");
	MESSAGE_TYPES.put(CON, "CON (" + CON + ")");
	MESSAGE_TYPES.put(NON, "NON (" + NON + ")");
	MESSAGE_TYPES.put(ACK, "ACK (" + ACK + ")");
	MESSAGE_TYPES.put(RST, "RST (" + RST + ")");
    }

    public static String asString(int messageType) {
	String result = MESSAGE_TYPES.get(messageType);
	return result == null ? "UNKOWN (" + messageType + ")" : result;
    }

    public static boolean isMessageType(int number) {
	return MESSAGE_TYPES.containsKey(number);
    }

}
