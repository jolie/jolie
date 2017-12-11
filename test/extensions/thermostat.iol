type TmpType: void { .id?: string } | int { .id?: string }

interface ThermostatInterface {
    OneWay: setTmp( TmpType )
    RequestResponse: 
        getTmp( TmpType )( int ),
        core( void )( string )
}