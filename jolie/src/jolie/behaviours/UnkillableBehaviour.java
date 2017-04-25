/*
 * Copyright (C) 2017 Fabrizio Montesi <famontesi@gmail.com>
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

/**
 *
 * @author Martin M. Andersen
 * @author Fabrizio Montesi
 */
public interface UnkillableBehaviour extends Behaviour
{
	@Override
	public default Behaviour clone( TransformationReason reason )
	{
		throw new UnsupportedOperationException( "Called clone on a behaviour that was not supposed to be cloned. (This is a bug in the Jolie interpreter: please report it!)" );
	}

	@Override
	public default boolean isKillable()
	{
		return false;
	}
}
