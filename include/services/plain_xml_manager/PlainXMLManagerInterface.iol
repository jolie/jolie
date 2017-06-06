
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

type GetNodeRequest: void {
  .resourceName: string
  .path: string
}

type ShowTreeResponse: void {
  .path*: string
}

type ResourceRequest: void {
  .resourceName: string
}


interface PlainXMLManagerInterface {
  RequestResponse:
    addTo( AddToRequest )( void ) throws ResourceDoesNotExist( string ) NodeDoesNoteExist( string ),
    createPlainXML( CreatePlainXMLRequest )( void ) throws ResourceAlreadyExists,
    destroyPlainXML( ResourceRequest )( void ) throws ResourceDoesNotExist,
    getElement( GetNodeRequest )( undefined ) throws ResourceDoesNotExist,
    getXMLString( ResourceRequest )( string ) throws ResourceDoesNotExist,
    showTree( ResourceRequest )( ShowTreeResponse ) throws ResourceDoesNotExist
}
