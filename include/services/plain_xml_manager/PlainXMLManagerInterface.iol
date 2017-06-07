
type XMLNodeContent: void {
    .Element?: void {
        .Name: string
        .Namespace: string
        .Prefix: string
        .Node*: XMLNodeContent
    }
    .CDATA?: void {
        .Value: string
    }
    .Text?: void {
        .Value: string
    }
    .Comment?: void {
        .Value: string
    }
    .Entity?: void {
        .Name: string
        .Value: string
        .Node*: XMLNodeContent
    }
    .EntityReference?: void {
        .Value: string
        .Node*: XMLNodeContent
    }
    .Notation?: void {
        .Value: string
    }
    .Attribute?: void {
        .Name: string
        .Value: string
        .Prefix: string
        .Namespace: string
    }
}

type XMLNodes: void {
    .Node*: XMLNodeContent
}

type AddToRequest: void {
    .from: XMLNodes
    .to: void {
        .resourceName: string
        .__path: string
        .__index: int
    }
}

type CreatePlainXMLRequest: void {
    .resourceName: string
    .xml: string
}

type GetElementRequest: void {
    .resourceName: string
    .__path: string
    .__index: int
}

type ShowTreeResponse: void {
    .path*: string
    .tree: undefined
}

type ResourceRequest: void {
    .resourceName: string
}


interface PlainXMLManagerInterface {
  RequestResponse:
    addElementTo( AddToRequest )( void )
          throws  ResourceDoesNotExist( string )
                  NodeDoesNoteExist( string )
                  PositionOrPathNotAvailable,
    createPlainXML( CreatePlainXMLRequest )( void ) throws ResourceAlreadyExists,
    destroyPlainXML( ResourceRequest )( void ) throws ResourceDoesNotExist,
    getElement( GetElementRequest )( undefined ) throws ResourceDoesNotExist,
    getXMLString( ResourceRequest )( string ) throws ResourceDoesNotExist,
    showTree( ResourceRequest )( ShowTreeResponse ) throws ResourceDoesNotExist
}
