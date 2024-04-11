/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

import jolie.process.TransformationReason;
import jolie.runtime.expression.Expression;

public class InstallFixedVariablePath implements Expression {
	final private VariablePath path;
	final private Value fixedEvaluation;

	public InstallFixedVariablePath( VariablePath path ) {
		this.path = path;
		this.fixedEvaluation = null;
	}

	private InstallFixedVariablePath( Value fixedEvaluation ) {
		this.path = null;
		this.fixedEvaluation = fixedEvaluation;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		if( reason instanceof HandlerInstallationReason ) {
			return new InstallFixedVariablePath( Value.createDeepCopy( path.getValue() ) );
		}

		return new InstallFixedVariablePath( path );
	}

	@Override
	public Value evaluate() {
		return fixedEvaluation;
	}
}
