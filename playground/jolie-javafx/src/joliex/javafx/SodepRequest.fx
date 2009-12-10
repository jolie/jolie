/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.javafx;

import joliex.javafx.impl.SodepRequestRunnable;
import javafx.async.JavaTaskBase;
import javafx.async.RunnableFuture;
import joliex.javafx.impl.SodepRequestListener;

/**
 * @author Fabrizio Montesi
 */
public class SodepRequest extends JavaTaskBase, SodepRequestListener
{
    public-init var location: String;
    public-init var operation: String;
    public-init var value: jolie.runtime.Value;

    public-init var onComplete: function(:jolie.runtime.Value):Void;
    public-init	var onFault: function(:jolie.runtime.FaultException):Void;
    public-init var onError: function(:java.lang.Exception):Void;

    protected override function create(): RunnableFuture
    {
	new SodepRequestRunnable(
	    location,
	    operation,
	    value,
	    this
	);
    }

    public override function _onComplete( value:jolie.runtime.Value )
    {
	FX.deferAction( function() { onComplete( value ); } );
    }

    public override function _onFault( fault:jolie.runtime.FaultException )
    {
	FX.deferAction( function() { onFault( fault ); } );
    }

    public override function _onError( e:java.lang.Exception )
    {
	FX.deferAction( function() { onError( e ); } );
    }
}
