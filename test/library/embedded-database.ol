/*
 * Copyright (C) 2024 Claudio Guidi <guidiclaudio@gmail.com>
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

from ..test-unit import TestUnitInterface
from database import Database
from runtime import Runtime
from string_utils import StringUtils
from file import File
from zip-utils import ZipUtils

interface ApacheDerbyDownloadInterface {
    RequestResponse:
        downloadDerby
}

service Main {

    embed Database as Database
    embed Runtime as Runtime
    embed StringUtils as StringUtils
    embed File as File
    embed ZipUtils as ZipUtils

    outputPort ApacheDerby1_10_14 {
        location: "socket://archive.apache.org:443/dist/db/derby/db-derby-10.14.2.0/"
        protocol: https {
            .osc.downloadDerby.alias = "db-derby-10.14.2.0-lib.zip"
            .osc.downloadDerby.method = "get"
        }
        interfaces: ApacheDerbyDownloadInterface
    }

    inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

    define initDatabase
    {
        scope( init_db ) {
            install( SQLException => throw( TestFailed, init_db.SQLException.stackTrace ) )
            i = 0
            q.statement[i++] = "create table test (
                name varchar(128) not null
            )"
            executeTransaction@Database( q )()
            undef( q )
        }
    }

    define connect_db {
            connectionInfo << {
                host = ""
                driver = "derby_embedded"
                port = 0
                database = "./data"
                username = ""
                password = ""
            };
            connect@Database( connectionInfo )()
    }

    main {
        test()() {
            downloadDerby@ApacheDerby1_10_14()( derby )
            writeFile@File( { filename = "zip.zip", content << derby } )()
            unzip@ZipUtils( { filename = "zip.zip", targetPath = "./unzip"} )()
            delete@File( "zip.zip" )()
            readFile@File( { filename = "unzip/db-derby-10.14.2.0-lib/lib/derby.jar", format = "binary" } )( derbyJar )
            writeFile@File( { filename = "derby.jar", format = "binary", content << derbyJar })()
            loadLibrary@Runtime( "derby.jar" )()

            scope( ConnectionScope ) {
                install( IOException => throw( TestFailed, ConnectionScope.IOException.stackTrace ) )
                install( ConnectionError =>
                    connectionInfo.attributes = "create=true"
                    connect@Database( connectionInfo )()
                    initDatabase
                    connect_db
                )
                install( SQLException =>
                    if ( ConnectionScope.SQLException.SQLState == "XJ004" && ConnectionScope.SQLException.errorCode == 40000 ) {
                        connectionInfo.attributes = "create=true"
                        connect@Database( connectionInfo )()
                        initDatabase
                        connect_db
                    } else {
                        valueToPrettyString@StringUtils( ConnectionScope.SQLException )( error )
                        throw( TestFailed, "SQLEXception critical error" )
                    }
                )

                connect_db
            }
            

            q = "INSERT INTO test (name) VALUES ('test')"
            update@Database( q )()

            q = "SELECT * FROM test"
            query@Database( q )( result )
            if ( #result.row != 1 ) {
                throw( TestFailed, "Wrong number of rows" )
            }
            if ( result.row[ 0 ].NAME != "test" ) {
               throw( TestFailed, "wrong reading from database" )
            }
            delete@File( "derby.jar" )()
            delete@File( "derby.log" )()
            deleteDir@File("./data")()
            deleteDir@File("./unzip")()
        }
    }
}