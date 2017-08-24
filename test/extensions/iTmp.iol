type TmpType: void | int { .id: int } | string | int

interface ThermostatInterface {
  OneWay: setTmp( TmpType )
  RequestResponse: getTmp( TmpType )( TmpType )
}