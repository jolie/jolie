package jolie.net.coap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Option {

    public enum Occurence {
	NONE, ONCE, MULTIPLE
    }

    public static final int UNKNOWN = -1;
    public static final int IF_MATCH = 1;
    public static final int URI_HOST = 3;
    public static final int ETAG = 4;
    public static final int IF_NONE_MATCH = 5;
    public static final int OBSERVE = 6;
    public static final int URI_PORT = 7;
    public static final int LOCATION_PATH = 8;
    public static final int URI_PATH = 11;
    public static final int CONTENT_FORMAT = 12;
    public static final int MAX_AGE = 14;
    public static final int URI_QUERY = 15;
    public static final int ACCEPT = 17;
    public static final int LOCATION_QUERY = 20;
    public static final int BLOCK_2 = 23;
    public static final int BLOCK_1 = 27;
    public static final int SIZE_2 = 28;
    public static final int PROXY_URI = 35;
    public static final int PROXY_SCHEME = 39;
    public static final int SIZE_1 = 60;
    public static final int ENDPOINT_ID_1 = 124;
    public static final int ENDPOINT_ID_2 = 189;
    private static HashMap<Integer, String> OPTIONS = new HashMap<>();

    static {

	OPTIONS.put(IF_MATCH, "IF MATCH (" + IF_MATCH + ")");
	OPTIONS.put(URI_HOST, "URI HOST (" + URI_HOST + ")");
	OPTIONS.put(ETAG, "ETAG (" + ETAG + ")");
	OPTIONS.put(IF_NONE_MATCH, "IF NONE MATCH (" + IF_NONE_MATCH + ")");
	OPTIONS.put(OBSERVE, "OBSERVE (" + OBSERVE + ")");
	OPTIONS.put(URI_PORT, "URI PORT (" + URI_PORT + ")");
	OPTIONS.put(LOCATION_PATH, "LOCATION PATH (" + LOCATION_PATH + ")");
	OPTIONS.put(URI_PATH, "URI PATH (" + URI_PATH + ")");
	OPTIONS.put(CONTENT_FORMAT, "CONTENT FORMAT (" + CONTENT_FORMAT + ")");
	OPTIONS.put(MAX_AGE, "MAX AGE (" + MAX_AGE + ")");
	OPTIONS.put(URI_QUERY, "URI QUERY (" + URI_QUERY + ")");
	OPTIONS.put(ACCEPT, "ACCEPT (" + ACCEPT + ")");
	OPTIONS.put(LOCATION_QUERY, "LOCATION QUERY (" + LOCATION_QUERY + ")");
	OPTIONS.put(BLOCK_2, "BLOCK 2 (" + BLOCK_2 + ")");
	OPTIONS.put(BLOCK_1, "BLOCK 1 (" + BLOCK_1 + ")");
	OPTIONS.put(SIZE_2, "SIZE 2 (" + SIZE_2 + ")");
	OPTIONS.put(PROXY_URI, "PROXY URI (" + PROXY_URI + ")");
	OPTIONS.put(PROXY_SCHEME, "PROXY SCHEME (" + PROXY_SCHEME + ")");
	OPTIONS.put(SIZE_1, "SIZE 1 (" + SIZE_1 + ")");
	OPTIONS.put(ENDPOINT_ID_1, "ENDPOINT ID 1 (" + ENDPOINT_ID_1 + ")");
	OPTIONS.put(ENDPOINT_ID_2, "ENDPOINT ID 2 (" + ENDPOINT_ID_2 + ")");
    }

    public static String asString(int optionNumber) {
	String result = OPTIONS.get(optionNumber);
	return result == null ? "UNKOWN (" + optionNumber + ")" : result;
    }

    private static Map<Integer, Set<Integer>> MUTUAL_EXCLUSIONS
	    = new HashMap<>();

    static {
	MUTUAL_EXCLUSIONS.put(URI_HOST, Collections.singleton(PROXY_URI));
	MUTUAL_EXCLUSIONS.put(PROXY_URI, Collections.singleton(URI_HOST));

	MUTUAL_EXCLUSIONS.put(URI_PORT, Collections.singleton(PROXY_URI));
	MUTUAL_EXCLUSIONS.get(PROXY_URI).add(URI_PORT);

	MUTUAL_EXCLUSIONS.put(URI_PATH, Collections.singleton(PROXY_URI));
	MUTUAL_EXCLUSIONS.get(PROXY_URI).add(URI_PATH);

	MUTUAL_EXCLUSIONS.put(URI_QUERY, Collections.singleton(PROXY_URI));
	MUTUAL_EXCLUSIONS.get(PROXY_URI).add(URI_QUERY);

	MUTUAL_EXCLUSIONS.put(PROXY_SCHEME, Collections.singleton(PROXY_URI));
	MUTUAL_EXCLUSIONS.get(PROXY_URI).add(PROXY_SCHEME);
    }

    public static boolean mutuallyExcludes(int firstOptionNumber,
	    int secondOptionNumber) {
	return MUTUAL_EXCLUSIONS.get(firstOptionNumber)
		.contains(secondOptionNumber);
    }

    private static final HashMap<Integer, HashMap<Integer, Option.Occurence>> OCCURENCE_CONSTRAINTS = new HashMap<>();

    static {

	// GET Requests
	HashMap<Integer, Option.Occurence> GET = new HashMap<>();
	GET.put(URI_HOST, Occurence.ONCE);
	GET.put(URI_PORT, Occurence.ONCE);
	GET.put(URI_PATH, Occurence.MULTIPLE);
	GET.put(URI_QUERY, Occurence.MULTIPLE);
	GET.put(PROXY_URI, Occurence.ONCE);
	GET.put(PROXY_SCHEME, Occurence.ONCE);
	GET.put(ACCEPT, Occurence.MULTIPLE);
	GET.put(ETAG, Occurence.MULTIPLE);
	GET.put(OBSERVE, Occurence.ONCE);
	GET.put(BLOCK_2, Occurence.ONCE);
	GET.put(SIZE_2, Occurence.ONCE);
	GET.put(ENDPOINT_ID_1, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.GET, GET);

	// POST Requests
	HashMap<Integer, Option.Occurence> POST = new HashMap<>();
	POST.put(URI_HOST, Occurence.ONCE);
	POST.put(URI_PORT, Occurence.ONCE);
	POST.put(URI_PATH, Occurence.MULTIPLE);
	POST.put(URI_QUERY, Occurence.MULTIPLE);
	POST.put(PROXY_URI, Occurence.ONCE);
	POST.put(PROXY_SCHEME, Occurence.ONCE);
	POST.put(ACCEPT, Occurence.MULTIPLE);
	POST.put(CONTENT_FORMAT, Occurence.ONCE);
	POST.put(BLOCK_2, Occurence.ONCE);
	POST.put(BLOCK_1, Occurence.ONCE);
	POST.put(SIZE_2, Occurence.ONCE);
	POST.put(SIZE_1, Occurence.ONCE);
	POST.put(ENDPOINT_ID_1, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.POST, POST);

	// PUT Requests
	HashMap<Integer, Option.Occurence> PUT = new HashMap<>();
	PUT.put(URI_HOST, Occurence.ONCE);
	PUT.put(URI_PORT, Occurence.ONCE);
	PUT.put(URI_PATH, Occurence.MULTIPLE);
	PUT.put(URI_QUERY, Occurence.MULTIPLE);
	PUT.put(PROXY_URI, Occurence.ONCE);
	PUT.put(PROXY_SCHEME, Occurence.ONCE);
	PUT.put(ACCEPT, Occurence.MULTIPLE);
	PUT.put(CONTENT_FORMAT, Occurence.ONCE);
	PUT.put(IF_MATCH, Occurence.ONCE);
	PUT.put(IF_NONE_MATCH, Occurence.ONCE);
	PUT.put(BLOCK_2, Occurence.ONCE);
	PUT.put(BLOCK_1, Occurence.ONCE);
	PUT.put(SIZE_2, Occurence.ONCE);
	PUT.put(SIZE_1, Occurence.ONCE);
	PUT.put(ENDPOINT_ID_1, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.PUT, PUT);

	// DELETE Requests
	HashMap<Integer, Option.Occurence> DELETE = new HashMap<>();
	DELETE.put(URI_HOST, Occurence.ONCE);
	DELETE.put(URI_PORT, Occurence.ONCE);
	DELETE.put(URI_PATH, Occurence.MULTIPLE);
	DELETE.put(URI_QUERY, Occurence.MULTIPLE);
	DELETE.put(PROXY_URI, Occurence.ONCE);
	DELETE.put(PROXY_SCHEME, Occurence.ONCE);
	DELETE.put(ENDPOINT_ID_1, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.DELETE, DELETE);

	//Response success (2.x)
	HashMap<Integer, Option.Occurence> CREATED_201 = new HashMap<>();
	CREATED_201.put(ETAG, Occurence.ONCE);
	CREATED_201.put(OBSERVE, Occurence.ONCE);
	CREATED_201.put(LOCATION_PATH, Occurence.MULTIPLE);
	CREATED_201.put(LOCATION_QUERY, Occurence.MULTIPLE);
	CREATED_201.put(CONTENT_FORMAT, Occurence.ONCE);
	CREATED_201.put(BLOCK_2, Occurence.ONCE);
	CREATED_201.put(BLOCK_1, Occurence.ONCE);
	CREATED_201.put(SIZE_2, Occurence.ONCE);
	CREATED_201.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.CREATED_201, CREATED_201);

	HashMap<Integer, Option.Occurence> DELETED_202 = new HashMap<>();
	DELETED_202.put(CONTENT_FORMAT, Occurence.ONCE);
	DELETED_202.put(BLOCK_2, Occurence.ONCE);
	DELETED_202.put(BLOCK_1, Occurence.ONCE);
	DELETED_202.put(SIZE_2, Occurence.ONCE);
	DELETED_202.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.DELETED_202, DELETED_202);

	HashMap<Integer, Option.Occurence> VALID_203 = new HashMap<>();
	VALID_203.put(OBSERVE, Occurence.ONCE);
	VALID_203.put(ETAG, Occurence.ONCE);
	VALID_203.put(MAX_AGE, Occurence.ONCE);
	VALID_203.put(CONTENT_FORMAT, Occurence.ONCE);
	VALID_203.put(ENDPOINT_ID_1, Occurence.ONCE);
	VALID_203.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.VALID_203, VALID_203);

	HashMap<Integer, Option.Occurence> CHANGED_204 = new HashMap<>();
	CHANGED_204.put(ETAG, Occurence.ONCE);
	CHANGED_204.put(CONTENT_FORMAT, Occurence.ONCE);
	CHANGED_204.put(BLOCK_2, Occurence.ONCE);
	CHANGED_204.put(BLOCK_1, Occurence.ONCE);
	CHANGED_204.put(SIZE_2, Occurence.ONCE);
	CHANGED_204.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.CHANGED_204, CHANGED_204);

	HashMap<Integer, Option.Occurence> CONTENT_205 = new HashMap<>();
	CONTENT_205.put(OBSERVE, Occurence.ONCE);
	CONTENT_205.put(ETAG, Occurence.ONCE);
	CONTENT_205.put(MAX_AGE, Occurence.ONCE);
	CONTENT_205.put(CONTENT_FORMAT, Occurence.ONCE);
	CONTENT_205.put(BLOCK_2, Occurence.ONCE);
	CONTENT_205.put(BLOCK_1, Occurence.ONCE);
	CONTENT_205.put(SIZE_2, Occurence.ONCE);
	CONTENT_205.put(ENDPOINT_ID_1, Occurence.ONCE);
	CONTENT_205.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.CONTENT_205, CONTENT_205);

	HashMap<Integer, Option.Occurence> CONTINUE_231 = new HashMap<>();
	CONTINUE_231.put(BLOCK_1, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.CONTINUE_231, CONTINUE_231);

	// Client ERROR Responses (4.x)
	HashMap<Integer, Option.Occurence> BAD_REQUEST_400 = new HashMap<>();
	BAD_REQUEST_400.put(MAX_AGE, Occurence.ONCE);
	BAD_REQUEST_400.put(CONTENT_FORMAT, Occurence.ONCE);
	BAD_REQUEST_400.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.BAD_REQUEST_400, BAD_REQUEST_400);

	HashMap<Integer, Option.Occurence> UNAUTHORIZED_401 = new HashMap<>();
	UNAUTHORIZED_401.put(MAX_AGE, Occurence.ONCE);
	UNAUTHORIZED_401.put(CONTENT_FORMAT, Occurence.ONCE);
	UNAUTHORIZED_401.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.UNAUTHORIZED_401, UNAUTHORIZED_401);

	HashMap<Integer, Option.Occurence> BAD_OPTION_402 = new HashMap<>();
	BAD_OPTION_402.put(MAX_AGE, Occurence.ONCE);
	BAD_OPTION_402.put(CONTENT_FORMAT, Occurence.ONCE);
	BAD_OPTION_402.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.BAD_OPTION_402, BAD_OPTION_402);

	HashMap<Integer, Option.Occurence> FORBIDDEN_403 = new HashMap<>();
	FORBIDDEN_403.put(MAX_AGE, Occurence.ONCE);
	FORBIDDEN_403.put(CONTENT_FORMAT, Occurence.ONCE);
	FORBIDDEN_403.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.FORBIDDEN_403, FORBIDDEN_403);

	HashMap<Integer, Option.Occurence> NOT_FOUND_404 = new HashMap<>();
	NOT_FOUND_404.put(MAX_AGE, Occurence.ONCE);
	NOT_FOUND_404.put(CONTENT_FORMAT, Occurence.ONCE);
	NOT_FOUND_404.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.NOT_FOUND_404, NOT_FOUND_404);

	HashMap<Integer, Option.Occurence> METHOD_NOT_ALLOWED_405 = new HashMap<>();
	METHOD_NOT_ALLOWED_405.put(MAX_AGE, Occurence.ONCE);
	METHOD_NOT_ALLOWED_405.put(CONTENT_FORMAT, Occurence.ONCE);
	METHOD_NOT_ALLOWED_405.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.METHOD_NOT_ALLOWED_405, METHOD_NOT_ALLOWED_405);

	HashMap<Integer, Option.Occurence> NOT_ACCEPTABLE_406 = new HashMap<>();
	NOT_ACCEPTABLE_406.put(MAX_AGE, Occurence.ONCE);
	NOT_ACCEPTABLE_406.put(CONTENT_FORMAT, Occurence.ONCE);
	NOT_ACCEPTABLE_406.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.NOT_ACCEPTABLE_406, NOT_ACCEPTABLE_406);

	HashMap<Integer, Option.Occurence> REQUEST_ENTITY_INCOMPLETE_408 = new HashMap<>();
	REQUEST_ENTITY_INCOMPLETE_408.put(CONTENT_FORMAT, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.REQUEST_ENTITY_INCOMPLETE_408, REQUEST_ENTITY_INCOMPLETE_408);

	HashMap<Integer, Option.Occurence> PRECONDITION_FAILED_412 = new HashMap<>();
	PRECONDITION_FAILED_412.put(MAX_AGE, Occurence.ONCE);
	PRECONDITION_FAILED_412.put(CONTENT_FORMAT, Occurence.ONCE);
	PRECONDITION_FAILED_412.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.PRECONDITION_FAILED_412, PRECONDITION_FAILED_412);

	HashMap<Integer, Option.Occurence> REQUEST_ENTITY_TOO_LARGE_413 = new HashMap<>();
	REQUEST_ENTITY_TOO_LARGE_413.put(MAX_AGE, Occurence.ONCE);
	REQUEST_ENTITY_TOO_LARGE_413.put(CONTENT_FORMAT, Occurence.ONCE);
	REQUEST_ENTITY_TOO_LARGE_413.put(BLOCK_1, Occurence.ONCE);
	REQUEST_ENTITY_TOO_LARGE_413.put(SIZE_1, Occurence.ONCE);
	REQUEST_ENTITY_TOO_LARGE_413.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.REQUEST_ENTITY_TOO_LARGE_413, REQUEST_ENTITY_TOO_LARGE_413);

	HashMap<Integer, Option.Occurence> UNSUPPORTED_CONTENT_FORMAT_415 = new HashMap<>();
	UNSUPPORTED_CONTENT_FORMAT_415.put(MAX_AGE, Occurence.ONCE);
	UNSUPPORTED_CONTENT_FORMAT_415.put(CONTENT_FORMAT, Occurence.ONCE);
	UNSUPPORTED_CONTENT_FORMAT_415.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.UNSUPPORTED_CONTENT_FORMAT_415, UNSUPPORTED_CONTENT_FORMAT_415);

	// Server ERROR Responses ( 5.x )
	HashMap<Integer, Option.Occurence> INTERNAL_SERVER_ERROR_500 = new HashMap<>();
	INTERNAL_SERVER_ERROR_500.put(MAX_AGE, Occurence.ONCE);
	INTERNAL_SERVER_ERROR_500.put(CONTENT_FORMAT, Occurence.ONCE);
	INTERNAL_SERVER_ERROR_500.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.INTERNAL_SERVER_ERROR_500, INTERNAL_SERVER_ERROR_500);

	HashMap<Integer, Option.Occurence> NOT_IMPLEMENTED_501 = new HashMap<>();
	NOT_IMPLEMENTED_501.put(MAX_AGE, Occurence.ONCE);
	NOT_IMPLEMENTED_501.put(CONTENT_FORMAT, Occurence.ONCE);
	NOT_IMPLEMENTED_501.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.NOT_IMPLEMENTED_501, NOT_IMPLEMENTED_501);

	HashMap<Integer, Option.Occurence> BAD_GATEWAY_502 = new HashMap<>();
	BAD_GATEWAY_502.put(MAX_AGE, Occurence.ONCE);
	BAD_GATEWAY_502.put(CONTENT_FORMAT, Occurence.ONCE);
	BAD_GATEWAY_502.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.BAD_GATEWAY_502, BAD_GATEWAY_502);

	HashMap<Integer, Option.Occurence> GATEWAY_TIMEOUT_504 = new HashMap<>();
	GATEWAY_TIMEOUT_504.put(MAX_AGE, Occurence.ONCE);
	GATEWAY_TIMEOUT_504.put(CONTENT_FORMAT, Occurence.ONCE);
	GATEWAY_TIMEOUT_504.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.GATEWAY_TIMEOUT_504, GATEWAY_TIMEOUT_504);

	HashMap<Integer, Option.Occurence> PROXYING_NOT_SUPPORTED_505 = new HashMap<>();
	PROXYING_NOT_SUPPORTED_505.put(MAX_AGE, Occurence.ONCE);
	PROXYING_NOT_SUPPORTED_505.put(CONTENT_FORMAT, Occurence.ONCE);
	PROXYING_NOT_SUPPORTED_505.put(ENDPOINT_ID_2, Occurence.ONCE);
	OCCURENCE_CONSTRAINTS.put(MessageCode.PROXYING_NOT_SUPPORTED_505, PROXYING_NOT_SUPPORTED_505);

    }

    public static Occurence getPermittedOccurrence(int optionNumber, int messageCode) {
	Occurence result = OCCURENCE_CONSTRAINTS.get(messageCode).get(optionNumber);
	return result == null ? Occurence.NONE : result;
    }

    public static boolean isCritical(int optionNumber) {
	return (optionNumber & 1) == 1;
    }

    public static boolean isSafe(int optionNumber) {
	return !((optionNumber & 2) == 2);
    }

    public static boolean isCacheKey(int optionNumber) {
	return !((optionNumber & 0x1e) == 0x1c);
    }
}
