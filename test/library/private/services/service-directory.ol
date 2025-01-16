from file import File

interface GetServiceDirectoryInterface {
    RequestResponse:
        getSerDir( void )( string )
}


service GetServiceDirectoryService {

    embed File as File

    inputPort IP {
        location: "local"
        interfaces: GetServiceDirectoryInterface
    }
    
    main {
        getSerDir( request )( response ) {
            response = getServiceDirectory@File()
        }
    }
}