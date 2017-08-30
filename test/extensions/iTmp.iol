type TmpType: void { .id: string } | int { .id: string } | int | void | string { .id: string }

interface ThermostatInterface {
  OneWay: setTmp( TmpType )
  RequestResponse: getTmp( TmpType )( TmpType )
}