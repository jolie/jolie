/**
 * *************************************************************************
 *   Copyright (C) 2006-2008 by Fabrizio Montesi <famontesi@gmail.com> * * This
 * program is free software; you can redistribute it and/or modify * it under
 * the terms of the GNU Library General Public License as * published by the
 * Free Software Foundation; either version 2 of the * License, or (at your
 * option) any later version. * * This program is distributed in the hope that
 * it will be useful, * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the *
 * GNU General Public License for more details. * * You should have received a
 * copy of the GNU Library General Public * License along with this program; if
 * not, write to the * Free Software Foundation, Inc., * 59 Temple Place - Suite
 * 330, Boston, MA 02111-1307, USA. * * For details about the authors of this
 * software, see the AUTHORS file. *
 **************************************************************************
 */
package jolie.behaviours;

/**
 *
 * @author martin
 */
public abstract class SimpleBehaviour implements Behaviour
{

	@Override
	public Behaviour clone( TransformationReason reason )
	{
		throw new UnsupportedOperationException( "Method has not been overridden." );
	}

	@Override
	public boolean isKillable()
	{
		return false;
	}
	
}
