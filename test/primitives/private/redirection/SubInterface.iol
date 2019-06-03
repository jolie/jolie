type SubRequest: void {
  .x: double
  .y: double
}

type SubResult: void {
  .result: double
}

interface SubInterface {
  RequestResponse:
    sub( SubRequest )( SubResult )
}
