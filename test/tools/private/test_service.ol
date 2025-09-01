
from console import Console
from file import File
from database import Database
from ini_utils import IniUtils
from runtime import Runtime



type GetAddressRequest: void {
	name: string
	surname: string
}

type GetAddressResponse: void {
	address: string
}

type GetPersonListRequest: void {
	full: bool
}


type GetPersonListResponse: void {
	person*: void {
		name: string 
		surname: string
		address?: string
	}
}

interface PeopleAddressServiceInterface {
RequestResponse:
	getAddress( GetAddressRequest )( GetAddressResponse ) throws PersonNotFound( void ),
	getPersonList( GetPersonListRequest )( GetPersonListResponse )
}

service PeopleAddressService {
	execution{ concurrent }

	embed Console as Console
	embed Database as Database
	embed File as File
	embed IniUtils as IniUtils
	embed Runtime as Runtime


	inputPort PeopleAddressService {
		Location: "local"
		Protocol: sodep
		Interfaces: PeopleAddressServiceInterface
	}


	main {

		[ getAddress( request )( response ) {
				q = "SELECT * FROM people WHERE name=:name AND surname=:surname"
				q.name = request.name
				q.surname = request.surname
				query@Database( q )( result )

				if ( #result.row == 0 ) { throw( PersonNotFound ) } 
				else { response.address = result.row[ 0 ].address }
		} ]

		[ getPersonList( request )( response ) {
				q = "SELECT * FROM people"
				query@Database( q )( result )

				for( r in result.row ) {
					if ( request.full ) {
						response.person[ #response.person ] << {
							name = r.name
							surname = r.surname
							address = r.address
						}
					} else {
						response.person[ #response.person ] << {
							name = r.name
							surname = r.surname
						}
					}
				}
		}]
 
	}
}
