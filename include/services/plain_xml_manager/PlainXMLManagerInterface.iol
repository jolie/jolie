
type AddToRequest: void {
  .from: void {
      .resourceName: string
      .path: string
  }
  .to: void {
      .resourceName: string
      .path: string
  }
}

type CreatePlainXMLRequest: void {
  .resourceName: string
  .xml: string
}

type DestroyPlainXMLRequest: void {
  .resourceName: string
}

type GetNodeRequest: void {
  .resourceName: string
  .path: string
}

type GetXMLStringRequest: void {
  .resourceName: string
}

interface PlainXMLManagerInterface {
  RequestResponse:
    addTo( AddToRequest )( void ) throws ResourceDoesNotExist( string ) NodeDoesNoteExist( string ),
    createPlainXML( CreatePlainXMLRequest )( void ) throws ResourceAlreadyExists,
    destroyPlainXML( DestroyPlainXMLRequest )( void ) throws ResourceDoesNotExist,
    getNode( GetNodeRequest )( undefined ) throws ResourceDoesNotExist,
    getXMLString( GetXMLStringRequest )( string ) throws ResourceDoesNotExist
}
