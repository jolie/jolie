/***************************************************************************
 *   Copyright (C) by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>  *
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

package joliex.util;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class SemaphoreUtils extends JavaService {
	
	private final HashMap< String, Semaphore > semaphores = new HashMap<String, Semaphore>();
	
	private String getSemaphoreName( Value v ){
		return v.getFirstChild( "name" ).strValue();
	}
	
	private int getPermits( Value v ){
		if( v.getFirstChild( "permits" ) != null && 
			v.getFirstChild( "permits" ).intValue() != 0 ){
			return v.getFirstChild( "permits" ).intValue();
		} else {
			return 1;
		}
	}
	
	private void checkSemaphore( String name ){
		synchronized( semaphores ){
			if( !semaphores.containsKey( name ) ){		
				semaphores.put( name , new Semaphore( 0 ));
			}
		}
	}
		
	public Boolean release( Value v ){
		String name = getSemaphoreName( v );
		int permits = getPermits( v );
		checkSemaphore( name );
		semaphores.get( name ).release( permits );
		return true;
	}
	
	public Boolean acquire( Value v ){
		String name = getSemaphoreName( v );
		int permits = getPermits( v );
		checkSemaphore( name );
		try {
			semaphores.get( name ).acquire( permits );
		} catch (InterruptedException ex) {
			Logger.getLogger(SemaphoreUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
		return true;
	}
	
}
