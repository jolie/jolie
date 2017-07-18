type TmpType: string | void | int { .id: int }

interface ThermostatInterface {
  OneWay: getTmp( TmpType ), setTmp( TmpType )
  RequestResponse: tmp( TmpType )(int )
}