/***************************************************************************
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
 *   Copyright (C) 2018 by Saverio Giallorenzo                             *
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

include "../AbstractTestUnit.iol"
include "console.iol"

include "private/weatherService.iol"
include "private/WS-testService.iol"

define testLocally {
  with ( command ){
    .args[#.args] = "-jar";
    .args[#.args] = "extensions/private/WS-test.jar";
    .args[#.args] = "http://localhost:14000/";
    .waitFor = 0
  };
  command = "java";
  exec@Exec( command )();
  sleep@Time( 500 )();
  req.x = 6;
  req.y = 11;
  sum@CalcServicePort( req )( res );
  if ( res.return != 6+11 ) {
    throw( TestFailed, "Wrong response from the SOAP Service" )
  };
  prod@CalcServicePort( req )( res );
  if ( res.return != 6*11 ) {
    throw( TestFailed, "Wrong response from the SOAP Service" )
  };
  close@CalcServicePort()()
}

define doTest
{
	scope( testRemoteServe )
  {
    install( IOException => 
      print@Console( "Couldn't find the SOAP server, testing locally\n\t\t\t" )(); 
      testLocally
    );
    with( request ) {
      .CityName = "Bolzano";
      .CountryName = "Italy"
    };
    GetWeather@GlobalWeatherSoap( request )( response );
    if ( !is_defined( response.GetWeatherResult ) ) {
      throw( TestFailed, "No webservice response" )
    }
  }
}
