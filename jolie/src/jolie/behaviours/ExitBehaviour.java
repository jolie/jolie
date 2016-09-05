/*
 * Copyright (C) 2007-2015 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.behaviours;

import jolie.Interpreter;
import jolie.StatefulContext;


public class ExitBehaviour implements Behaviour
{
	private ExitBehaviour(){}
	
	private static class LazyHolder {
		private LazyHolder() {}
		private static final ExitBehaviour instance = new ExitBehaviour();
	}
	
	static public ExitBehaviour getInstance()
	{
		return ExitBehaviour.LazyHolder.instance;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		return getInstance();
	}
	
	@Override
	public void run(StatefulContext ctx)
	{
		final Interpreter interpreter = Interpreter.getInstance();
		interpreter.execute( () -> interpreter.exit() );
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}