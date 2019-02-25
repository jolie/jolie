/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.email.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author maschio
 */
public class SmtpMessage
{
	/** Headers: Map of List of String hashed on header name. */
	private Map<String, List<String>> headers;
	/** Message body. */
	private StringBuilder body;

	/** Constructor. Initializes headers Map and body buffer. */
	public SmtpMessage() {
		headers = new LinkedHashMap<>(10);
		body = new StringBuilder();
	}

	/**
	 * Update the headers or body depending on the SmtpResponse object and line of input.
	 *
	 * @param response SmtpResponse object
	 * @param params   remainder of input line after SMTP command has been removed
	 */
	public void store(SmtpResponse response, String params) {
		if (params != null) {
			if (SmtpState.DATA_HDR.equals(response.getNextState())) {
				int headerNameEnd = params.indexOf(':');
				if (headerNameEnd >= 0) {
					String name = params.substring(0, headerNameEnd).trim();
					String value = params.substring(headerNameEnd + 1).trim();
					addHeader(name, value);
				}
			} else if (SmtpState.DATA_BODY == response.getNextState()) {
				body.append(params);
			}
		}
	}

	/**
	 * Get an Iterator over the header names.
	 *
	 * @return an Iterator over the set of header names (String)
	 */
	public Set<String> getHeaderNames() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(headers.keySet()));
	}

	/**
	 * Get the value(s) associated with the given header name.
	 *
	 * @param name header name
	 * @return value(s) associated with the header name
	 */
	public List<String> getHeaderValues(String name) {
		List<String> values = headers.get(name);
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(new ArrayList<>(values));
		}
	}

	/**
	 * Get the first values associated with a given header name.
	 *
	 * @param name header name
	 * @return first value associated with the header name
	 */
	public String getHeaderValue(String name) {
		List<String> values = headers.get(name);
		if (values == null) {
			return null;
		} else {
			return values.get(0);
		}
	}

	/**
	 * Get the message body.
	 *
	 * @return message body
	 */
	public String getBody() {
		return body.toString();
	}

	/**
	 * Adds a header to the Map.
	 *
	 * @param name  header name
	 * @param value header value
	 */
	private void addHeader(String name, String value) {
		List<String> valueList = headers.get(name);
		if (valueList == null) {
			valueList = new ArrayList<>(1);
			headers.put(name, valueList);
		}
		valueList.add(value);
	}

	/**
	 * String representation of the SmtpMessage.
	 *
	 * @return a String
	 */
	@Override
	public String toString() {
		StringBuilder msg = new StringBuilder();
		for (Map.Entry<String, List<String>> stringListEntry : headers.entrySet()) {
			for (String value : stringListEntry.getValue()) {
				msg.append(stringListEntry.getKey());
				msg.append(": ");
				msg.append(value);
				msg.append('\n');
			}
		}
		msg.append('\n');
		msg.append(body);
		msg.append('\n');
		return msg.toString();
	}	
}
