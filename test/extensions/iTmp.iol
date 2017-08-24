type TmpType: void | int { .id: int }

interface ThermostatInterface {
  OneWay: setTmp( TmpType )
  RequestResponse: getTmp( TmpType )( int )
}