/***************************************************************************
 *   Copyright (C) 2007-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/


package jolie.runtime;


import jolie.runtime.typing.Type;

/**
 * @author Fabrizio Montesi
 *
 */
public abstract class InputOperation extends AbstractIdentifiableObject {
	/**
	 * Constructor
	 *
	 * @param id the name that identified this input operation
	 */
	public InputOperation( String id ) {
		super( id );
	}

	public abstract Type requestType();

	/**
	 * Receives a message from CommCore and passes it to the right InputProcess. If no suitable
	 * InputProcess is found, the message is enqueued in memory.
	 *
	 * @param channel The channel which received the message. Useful if the operation wants to send a
	 *        response.
	 * @param message The received message.
	 */
	/*
	 * public synchronized void recvMessage( CommChannel channel, final CommMessage message ) {
	 * VariablePath path = null; InputProcessExecution pe = null; for( Entry< InputProcessExecution,
	 * ExecutionThread > entry : procsMap.entrySet() ) { pe = entry.getKey(); if ( pe instanceof
	 * NDChoiceProcess.Execution ) { path = ((NDChoiceProcess.Execution)pe).inputVarPath(
	 * message.operationName() ); } else if ( pe.parent() instanceof InputOperationProcess ) { path =
	 * ((InputOperationProcess)pe.parent()).inputVarPath(); }
	 *
	 * CommChannelHandler.currentThread().setExecutionThread( entry.getValue() ); if (
	 * entry.getValue().checkCorrelation( path, message ) ) { try { if ( entry.getKey().recvMessage(
	 * channel, message ) ) { procsMap.remove( entry.getKey() ); return; } } catch(
	 * TypeCheckingException e ) { return; } } }
	 *
	 * final Pair< CommChannel, CommMessage > pair = new Pair< CommChannel, CommMessage >( channel,
	 * message ); mesgList.add( pair ); final Interpreter interpreter = Interpreter.getInstance();
	 * interpreter.addTimeoutHandler( new TimeoutHandler( interpreter.inputMessageTimeout() ) {
	 *
	 * @Override public void onTimeout() { boolean removed; synchronized( this ) { removed =
	 * mesgList.remove( pair ); } if ( removed && interpreter.verbose() ) { interpreter.logInfo(
	 * "Message " + message.id() + " discarded for timeout" ); } } } ); }
	 */
}
