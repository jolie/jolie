include "locations.iol"
include "SubInterface.iol"

execution { concurrent }

inputPort Sub {
  Location: Location_Sub
  Protocol: http
  Interfaces: SubInterface
}

main {
  sub( request )( response ) {
    response.result = request.x - request.y
  }
}
