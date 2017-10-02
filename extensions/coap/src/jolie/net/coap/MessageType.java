package jolie.net.coap;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

public abstract class MessageType {

    public static final int CON = 0;
    public static final int NON = 1;
    public static final int ACK = 2;
    public static final int RST = 3;

    private static final HashMap<Integer, String> MESSAGE_TYPES
	    = new HashMap<>();

    static {
	MESSAGE_TYPES.putAll(ImmutableMap.<Integer, String>builder()
		.put(CON, "CON (" + CON + ")")
		.put(NON, "NON (" + NON + ")")
		.put(ACK, "ACK (" + ACK + ")")
		.put(RST, "RST (" + RST + ")")
		.build());
    }

    public static String asString(int messageType) {
	String result = MESSAGE_TYPES.get(messageType);
	return result == null ? "UNKOWN (" + messageType + ")" : result;
    }

    public static boolean isMessageType(int number) {
	return MESSAGE_TYPES.containsKey(number);
    }

}
