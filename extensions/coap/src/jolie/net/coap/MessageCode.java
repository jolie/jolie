package jolie.net.coap;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;

public abstract class MessageCode {

    public static final int EMPTY = 0;
    public static final int GET = 1;
    public static final int POST = 2;
    public static final int PUT = 3;
    public static final int DELETE = 4;
    public static final int CREATED_201 = 65;
    public static final int DELETED_202 = 66;
    public static final int VALID_203 = 67;
    public static final int CHANGED_204 = 68;
    public static final int CONTENT_205 = 69;
    public static final int CONTINUE_231 = 95;
    public static final int BAD_REQUEST_400 = 128;
    public static final int UNAUTHORIZED_401 = 129;
    public static final int BAD_OPTION_402 = 130;
    public static final int FORBIDDEN_403 = 131;
    public static final int NOT_FOUND_404 = 132;
    public static final int METHOD_NOT_ALLOWED_405 = 133;
    public static final int NOT_ACCEPTABLE_406 = 134;
    public static final int REQUEST_ENTITY_INCOMPLETE_408 = 136;
    public static final int PRECONDITION_FAILED_412 = 140;
    public static final int REQUEST_ENTITY_TOO_LARGE_413 = 141;
    public static final int UNSUPPORTED_CONTENT_FORMAT_415 = 143;
    public static final int INTERNAL_SERVER_ERROR_500 = 160;
    public static final int NOT_IMPLEMENTED_501 = 161;
    public static final int BAD_GATEWAY_502 = 162;
    public static final int SERVICE_UNAVAILABLE_503 = 163;
    public static final int GATEWAY_TIMEOUT_504 = 164;
    public static final int PROXYING_NOT_SUPPORTED_505 = 165;

    private static final HashMap<Integer, String> MESSAGE_CODES
	    = new HashMap<>();

    static {

	MESSAGE_CODES.putAll(ImmutableMap.<Integer, String>builder()
		.put(EMPTY, "EMPTY (" + EMPTY + ")")
		.put(GET, "GET (" + GET + ")")
		.put(POST, "POST (" + POST + ")")
		.put(PUT, "PUT (" + PUT + ")")
		.put(DELETE, "DELETE (" + DELETE + ")")
		.put(CREATED_201, "CREATED (" + CREATED_201 + ")")
		.put(DELETED_202, "DELETED (" + DELETED_202 + ")")
		.put(VALID_203, "VALID (" + VALID_203 + ")")
		.put(CHANGED_204, "CHANGED (" + CHANGED_204 + ")")
		.put(CONTENT_205, "CONTENT (" + CONTENT_205 + ")")
		.put(CONTINUE_231, "CONTINUE (" + CONTINUE_231 + ")")
		.put(BAD_REQUEST_400, "BAD REQUEST (" + BAD_REQUEST_400 + ")")
		.put(UNAUTHORIZED_401, "UNAUTHORIZED (" + UNAUTHORIZED_401
			+ ")")
		.put(BAD_OPTION_402, "BAD OPTION (" + BAD_OPTION_402 + ")")
		.put(FORBIDDEN_403, "FORBIDDEN (" + FORBIDDEN_403 + ")")
		.put(NOT_FOUND_404, "NOT FOUND (" + NOT_FOUND_404 + ")")
		.put(METHOD_NOT_ALLOWED_405, "METHOD NOT ALLOWED ("
			+ METHOD_NOT_ALLOWED_405 + ")")
		.put(NOT_ACCEPTABLE_406, "NOT ACCEPTABLE ("
			+ NOT_ACCEPTABLE_406 + ")")
		.put(REQUEST_ENTITY_INCOMPLETE_408, "REQUEST ENTITY INCOMPLETE "
			+ "(" + REQUEST_ENTITY_INCOMPLETE_408 + ")")
		.put(PRECONDITION_FAILED_412, "PRECONDITION FAILED ("
			+ PRECONDITION_FAILED_412 + ")")
		.put(REQUEST_ENTITY_TOO_LARGE_413, "REQUEST ENTITY TOO LARGE ("
			+ REQUEST_ENTITY_TOO_LARGE_413 + ")")
		.put(UNSUPPORTED_CONTENT_FORMAT_415, "UNSUPPORTED CONTENT "
			+ "FORMAT (" + UNSUPPORTED_CONTENT_FORMAT_415 + ")")
		.put(INTERNAL_SERVER_ERROR_500, "INTERNAL SERVER ERROR ("
			+ INTERNAL_SERVER_ERROR_500 + ")")
		.put(NOT_IMPLEMENTED_501, "NOT IMPLEMENTED ("
			+ NOT_IMPLEMENTED_501 + ")")
		.put(BAD_GATEWAY_502, "BAD GATEWAY (" + BAD_GATEWAY_502 + ")")
		.put(SERVICE_UNAVAILABLE_503, "SERVICE UNAVAILABLE ("
			+ SERVICE_UNAVAILABLE_503 + ")")
		.put(GATEWAY_TIMEOUT_504, "GATEWAY TIMEOUT ("
			+ GATEWAY_TIMEOUT_504 + ")")
		.put(PROXYING_NOT_SUPPORTED_505, "PROXYING NOT SUPPORTED ("
			+ PROXYING_NOT_SUPPORTED_505 + ")")
		.build());
    }

    public static String asString(int messageCode) {
	String result = MESSAGE_CODES.get(messageCode);
	return result == null ? "UNKOWN (" + messageCode + ")" : result;
    }

    public static boolean isMessageCode(int number) {
	return MESSAGE_CODES.containsKey(number);
    }

    public static boolean isRequest(int messageCode) {
	return (messageCode > 0 && messageCode < 5);
    }

    public static boolean isResponse(int messageCode) {
	return messageCode >= 5;
    }

    public static boolean isErrorMessage(int codeNumber) {
	return (codeNumber >= 128);
    }

    public static boolean allowsContent(int codeNumber) {
	return !(codeNumber == GET || codeNumber == DELETE);
    }

}
