type TmpType: void | int | string | int { .id: int } | string { .id: int } | void { .id:int } | int { .id: string } | string { .id: string }

interface ThermostatInterface {
  OneWay: setTmp( TmpType )
  RequestResponse: getTmp( TmpType )( TmpType )
}