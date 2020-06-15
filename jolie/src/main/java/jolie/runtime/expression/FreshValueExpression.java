/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.runtime.expression;

import java.util.UUID;
import jolie.process.TransformationReason;
import jolie.runtime.Value;

public class FreshValueExpression implements Expression {
	private FreshValueExpression() {}

	private static class LazyHolder {
		private LazyHolder() {}

		static final FreshValueExpression instance = new FreshValueExpression();
	}

	public Expression cloneExpression( TransformationReason reason ) {
		return this;
	}

	public static FreshValueExpression getInstance() {
		return FreshValueExpression.LazyHolder.instance;
	}

	public Value evaluate() {
		return Value.create( UUID.randomUUID().toString() );
	}
}
