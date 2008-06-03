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

package jolie.net;

import java.util.List;
import java.util.Vector;

public class ListCommChannel extends CommChannel
{
	final protected List< CommMessage > ilist, olist;
	
	public ListCommChannel( List< CommMessage > ilist, List< CommMessage > olist )
	{
		this.ilist = ilist;
		this.olist = olist;
	}
	
	public ListCommChannel()
	{
		this.ilist = new Vector< CommMessage >();
		this.olist = new Vector< CommMessage >();
	}
	
	public List< CommMessage > inputList()
	{
		return ilist;
	}
	
	public List< CommMessage > outputList()
	{
		return olist;
	}
	
	public void send( CommMessage message )
	{
		synchronized( olist ) {
			olist.add( message );
			olist.notifyAll();
		}
	}
	
	public CommMessage recv()
	{
		CommMessage ret = null;
		synchronized( ilist ) {
			try {
				while( ilist.isEmpty() )
					ilist.wait();
			} catch( InterruptedException ie ) {}
			ret = ilist.remove( 0 );
		}
		return ret;
	}
	
	protected void closeImpl()
	{}
}
