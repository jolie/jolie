type TmpType: string | void | int { .id: int }

interface iTmp {
  OneWay: getTmp( TmpType ), setTmp( TmpType )
  RequestResponse: tmp( TmpType )(TmpType )
}