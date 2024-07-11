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

package jolie.process;

/**
 * No-op process.
 *
 * @author Fabrizio Montesi
 */
public class NullProcess implements Process {
	private NullProcess() {}

	private static class LazyHolder {
		private static final NullProcess INSTANCE = new NullProcess();
	}

	static public NullProcess getInstance() {
		return NullProcess.LazyHolder.INSTANCE;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return this;
	}

	@Override
	public void run() {}

	@Override
	public boolean isKillable() {
		return true;
	}
}
