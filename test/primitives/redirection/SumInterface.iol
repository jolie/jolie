type SumRequest: void {
  .x: double
  .y: double
}

type SumResult: void {
  .result: double
}

interface SumInterface {
  RequestResponse:
    sum( SumRequest )( SumResult )
}
