include "locations.iol"
include "SumInterface.iol"

execution{ concurrent }

inputPort Sum {
  Location: Location_Sum
  Protocol: http
  Interfaces: SumInterface
}

main {
  sum( request )( response ) {
    response.result = request.x + request.y
  }
}
