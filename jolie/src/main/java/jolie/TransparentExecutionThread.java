/***************************************************************************
 *   Copyright (C) 2015 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie;

import java.util.Map;
import java.util.concurrent.Future;

import jolie.net.SessionMessage;
import jolie.runtime.InputOperation;

/**
 *
 * @author Fabrizio Montesi
 */
public abstract class TransparentExecutionThread extends ExecutionThread {
	public TransparentExecutionThread( jolie.process.Process process, ExecutionThread parent ) {
		super( process, parent );
	}

	@Override
	public jolie.State state() {
		return parent.state();
	}

	@Override
	public Future< SessionMessage > requestMessage( InputOperation operation, ExecutionThread ethread ) {
		return parent.requestMessage( operation, ethread );
	}

	@Override
	public Future< SessionMessage > requestMessage( Map< String, InputOperation > operations,
		ExecutionThread ethread ) {
		return parent.requestMessage( operations, ethread );
	}

	@Override
	public String getSessionId() {
		return parent.getSessionId();
	}
}
