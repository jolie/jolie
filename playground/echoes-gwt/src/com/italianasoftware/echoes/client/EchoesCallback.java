/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as               *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public             *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package com.italianasoftware.echoes.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import joliex.gwt.client.FaultException;
import joliex.gwt.client.JolieCallback;
import joliex.gwt.client.Value;

abstract public class EchoesCallback extends JolieCallback
{
	private void displayFault( String faultString )
	{
		final DialogBox dialog = new DialogBox();
		dialog.add( new Label( faultString ) );
		Button closeButton = new Button( "Close" );
		closeButton.addClickListener( new ClickListener() {
			public void onClick( Widget arg0 ) {
				dialog.hide();
			}
		} );
		dialog.center();
		dialog.show();
	}
	
	public void onFault( FaultException fault )
	{
		displayFault( fault.faultName() + ": " + fault.value().strValue() );
	}

	public void onError( Throwable t )
	{
		displayFault( t.getMessage() );
	}
	
	abstract public void onSuccess( Value response );
}
