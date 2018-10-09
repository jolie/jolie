/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net.coap.message.options;

import java.util.Arrays;

/**
 * An empty option achieves it's goal just by being present in a message or not.
 * However, as the internal representation of options needs an instance of
 * {@link OptionValue} empty options are represented using
 * {@link EmptyOptionValue}.
 *
 * @author Oliver Kleine
 */
public final class EmptyOptionValue extends OptionValue<Void> {

  /**
   * @param optionNumber the option number of the {@link EmptyOptionValue} to be
   * created
   *
   * @throws java.lang.IllegalArgumentException if the given option number does
   * not refer to an empty option
   */
  public EmptyOptionValue(int optionNumber) throws IllegalArgumentException {
    super(optionNumber, new byte[0], false);
  }

  /**
   * Returns <code>null</code>
   *
   * @return <code>null</code>
   */
  @Override
  public Void getDecodedValue() {
    return null;
  }

  /**
   * Returns <code>0</code>
   *
   * @return <code>0</code>
   */
  @Override
  public int hashCode() {
    return 0;
  }

  /**
   * Checks if a given {@link Object} equals this {@link EmptyOptionValue}
   * instance. A given {@link Object} equals this {@link EmptyOptionValue} if
   * and only if the {@link Object} is an instance of {@link EmptyOptionValue}.
   *
   * @param object the object to check for equality with this instance of
   * {@link EmptyOptionValue}
   *
   * @return <code>true</code> if the given {@link Object} is an instance of
   * {@link EmptyOptionValue} and <code>false</code> otherwise.
   */
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof EmptyOptionValue)) {
      return false;
    }

    EmptyOptionValue other = (EmptyOptionValue) object;
    return Arrays.equals(this.getValue(), other.getValue());
  }
}
