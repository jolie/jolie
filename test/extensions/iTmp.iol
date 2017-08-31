type TmpType: void { .id: string } | string { .id: string } | void

interface ThermostatInterface {
  OneWay: setTmp( TmpType )
  RequestResponse: getTmp( TmpType )( TmpType )
}