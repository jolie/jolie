/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.email.support;

/**
 *
 * @author maschio
 */
public class SmtpResponse
{
		/** Response code - see RFC-2821. */
	private int code;
	/** Response message. */
	private String message;
	/** New state of the SMTP server once the request has been executed. */
	private SmtpState nextState;

	/**
	 * Constructor.
	 * @param code response code
	 * @param message response message
	 * @param next next state of the SMTP server
	 */
	SmtpResponse(int code, String message, SmtpState next) {
		this.code = code;
		this.message = message;
		this.nextState = next;
	}

	/**
	 * Get the response code.
	 * @return response code
	 */
	int getCode() {
		return code;
	}

	/**
	 * Get the response message.
	 * @return response message
	 */
	String getMessage() {
		return message;
	}

	/**
	 * Get the next SMTP server state.
	 * @return state
	 */
	SmtpState getNextState() {
		return nextState;
	}
}
