/*
 * Copyright (C) 2019 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

include "../AbstractTestUnit.iol"
include "console.iol"
include "string_utils.iol"

define doTest
{
  b = "hello"
  with( root ) {
    ..b = b
    ..c = "yes"
    ..a.left << "Left" {
      x = 1
      y = 2
      y.left = "y_l"
      y.right = "y_r"
    }
  }

  if ( root.b != "hello")  {
    throw( TestFailed, "root.b does not match expected content" )
  }
  if ( root.a.left.y.right != "y_r" ) {
    throw( TestFailed, "root.a.left.y.right does not match expected content" )
  }

  with( otherRoot ) {
    ..alias -> root.a
  }

  if ( otherRoot.alias.left.y.right != root.a.left.y.right ) {
    throw( TestFailed, "otherRoot.alias.left.y.right does not match expected content" )
  }
}
