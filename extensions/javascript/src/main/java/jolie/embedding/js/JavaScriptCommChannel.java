/***************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.embedding.js;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.script.Invocable;
import javax.script.ScriptException;

import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.PollableCommChannel;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;

/**
 * @author Fabrizio Montesi
 * 
 *         TODO: this shouldn't be polled
 */
public class JavaScriptCommChannel extends CommChannel implements PollableCommChannel {
	private final Invocable invocable;
	private final Map< Long, CommMessage > messages = new ConcurrentHashMap<>();
	private final Object json;

	private final static class JsonMethods {
		private final static String STRINGIFY = "stringify", PARSE = "parse";
	}

	public JavaScriptCommChannel( Invocable invocable, Object json ) {
		this.invocable = invocable;
		this.json = json;
	}

	@Override
	public CommChannel createDuplicate() {
		return new JavaScriptCommChannel( invocable, json );
	}

	@Override
	protected void sendImpl( CommMessage message )
		throws IOException {
		Object returnValue = null;
		try {
			StringBuilder builder = new StringBuilder();
			JsUtils.valueToJsonString( message.value(), true, Type.UNDEFINED, builder );
			Object param = invocable.invokeMethod( json, JsonMethods.PARSE, builder.toString() );
			returnValue = invocable.invokeFunction( message.operationName(), param );
		} catch( ScriptException | NoSuchMethodException e ) {
			throw new IOException( e );
		}

		CommMessage response;
		if( returnValue != null ) {
			Value value = Value.create();

			if( returnValue instanceof Value ) {
				value.refCopy( (Value) returnValue );
			} else {
				try {
					Object s = invocable.invokeMethod( json, JsonMethods.STRINGIFY, returnValue );
					JsUtils.parseJsonIntoValue( new StringReader( (String) s ), value, true );
				} catch( ScriptException | NoSuchMethodException e ) {
					// TODO: do something here, maybe encode an internal server error
				}

				value.setValue( returnValue );
			}

			response = new CommMessage(
				message.requestId(),
				message.operationName(),
				message.resourcePath(),
				value,
				null,
				null );
		} else {
			response = CommMessage.createEmptyResponse( message );
		}

		messages.put( message.requestId(), response );
	}

	@Override
	protected CommMessage recvImpl()
		throws IOException {
		throw new IOException( "Unsupported operation" );
	}

	@Override
	public Future< CommMessage > recvResponseFor( CommMessage request )
		throws IOException {
		return CompletableFuture.completedFuture( messages.remove( request.requestId() ) );
	}

	@Override
	protected void disposeForInputImpl()
		throws IOException {
		Interpreter.getInstance().commCore().registerForPolling( this );
	}

	@Override
	protected void closeImpl() {}

	@Override
	public boolean isReady() {
		return (!messages.isEmpty());
	}
}
