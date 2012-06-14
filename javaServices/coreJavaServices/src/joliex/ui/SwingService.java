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

package joliex.ui;

import javax.swing.JOptionPane;
import jolie.runtime.JavaService;
import jolie.runtime.embedding.RequestResponse;

public class SwingService extends JavaService
{
	@RequestResponse
	public void showMessageDialog( String message )
	{
		JOptionPane.showMessageDialog( null, message );
	}

	public Integer showYesNoQuestionDialog( String message )
	{
		return JOptionPane.showOptionDialog(
			null, message, "", JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE, null, null, null
		);
	}

	public String showInputDialog( String message )
	{
		return JOptionPane.showInputDialog( message );
	}
}
