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
public class SmtpActionType
{
		private byte value;

	/** Internal representation of the CONNECT action. */
	private static final byte CONNECT_BYTE = (byte) 1;
	/** Internal representation of the EHLO action. */
	private static final byte EHLO_BYTE = (byte) 2;
	/** Internal representation of the MAIL FROM action. */
	private static final byte MAIL_BYTE = (byte) 3;
	/** Internal representation of the RCPT action. */
	private static final byte RCPT_BYTE = (byte) 4;
	/** Internal representation of the DATA action. */
	private static final byte DATA_BYTE = (byte) 5;
	/** Internal representation of the DATA END (.) action. */
	private static final byte DATA_END_BYTE = (byte) 6;
	/** Internal representation of the QUIT action. */
	private static final byte QUIT_BYTE = (byte) 7;
	/** Internal representation of an unrecognized action: body text gets this action type. */
	private static final byte UNREC_BYTE = (byte) 8;
	/** Internal representation of the blank line action: separates headers and body text. */
	private static final byte BLANK_LINE_BYTE = (byte) 9;

	/** Internal representation of the stateless RSET action. */
	private static final byte RSET_BYTE = (byte) -1;
	/** Internal representation of the stateless VRFY action. */
	private static final byte VRFY_BYTE = (byte) -2;
	/** Internal representation of the stateless EXPN action. */
	private static final byte EXPN_BYTE = (byte) -3;
	/** Internal representation of the stateless HELP action. */
	private static final byte HELP_BYTE = (byte) -4;
	/** Internal representation of the stateless NOOP action. */
	private static final byte NOOP_BYTE = (byte) -5;

	/** CONNECT action. */
	static final SmtpActionType CONNECT = new SmtpActionType(CONNECT_BYTE);
	/** EHLO action. */
	static final SmtpActionType EHLO = new SmtpActionType(EHLO_BYTE);
	/** MAIL action. */
	static final SmtpActionType MAIL = new SmtpActionType(MAIL_BYTE);
	/** RCPT action. */
	static final SmtpActionType RCPT = new SmtpActionType(RCPT_BYTE);
	/** DATA action. */
	static final SmtpActionType DATA = new SmtpActionType(DATA_BYTE);
	/** "." action. */
	static final SmtpActionType DATA_END = new SmtpActionType(DATA_END_BYTE);
	/** Body text action. */
	static final SmtpActionType UNRECOG = new SmtpActionType(UNREC_BYTE);
	/** QUIT action. */
	static final SmtpActionType QUIT = new SmtpActionType(QUIT_BYTE);
	/** Header/body separator action. */
	static final SmtpActionType BLANK_LINE = new SmtpActionType(BLANK_LINE_BYTE);

	/** Stateless RSET action. */
	static final SmtpActionType RSET = new SmtpActionType(RSET_BYTE);
	/** Stateless VRFY action. */
	static final SmtpActionType VRFY = new SmtpActionType(VRFY_BYTE);
	/** Stateless EXPN action. */
	static final SmtpActionType EXPN = new SmtpActionType(EXPN_BYTE);
	/** Stateless HELP action. */
	static final SmtpActionType HELP = new SmtpActionType(HELP_BYTE);
	/** Stateless NOOP action. */
	static final SmtpActionType NOOP = new SmtpActionType(NOOP_BYTE);

	/**
	 * Create a new SMTP action type. Private to ensure no invalid values.
	 *
	 * @param value one of the _BYTE values
	 */
	private SmtpActionType(byte value) {
		this.value = value;
	}

	/**
	 * Indicates whether the action is stateless or not.
	 *
	 * @return true iff the action is stateless
	 */
	boolean isStateless() {
		return value < 0;
	}

	/**
	 * String representation of this SMTP action type.
	 *
	 * @return a String
	 */
	@Override
	public String toString() {
		switch (value) {
			case CONNECT_BYTE:
				return "Connect";
			case EHLO_BYTE:
				return "EHLO";
			case MAIL_BYTE:
				return "MAIL";
			case RCPT_BYTE:
				return "RCPT";
			case DATA_BYTE:
				return "DATA";
			case DATA_END_BYTE:
				return ".";
			case QUIT_BYTE:
				return "QUIT";
			case RSET_BYTE:
				return "RSET";
			case VRFY_BYTE:
				return "VRFY";
			case EXPN_BYTE:
				return "EXPN";
			case HELP_BYTE:
				return "HELP";
			case NOOP_BYTE:
				return "NOOP";
			case UNREC_BYTE:
				return "Unrecognized command / data";
			case BLANK_LINE_BYTE:
				return "Blank line";
			default:
				return "Unknown";
		}
	}
}
