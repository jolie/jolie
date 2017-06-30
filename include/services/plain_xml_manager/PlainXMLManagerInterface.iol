

type XMLNodeELement: void {
    .Name: any            // from version 1.6.0 and greater, it must be replaced with void | string
    .Namespace: any
    .Prefix: any
    .Node*: XMLNodeContent
}
type XMLNodeContent: void {
    .Element?: XMLNodeELement
    .CDATA?: void {
        .Value: any
    }
    .Text?: void {
        .Value: any
    }
    .Comment?: void {
        .Value: any
    }
    .Entity?: void {
        .Name: any
        .Value: any
        .Node*: XMLNodeContent
    }
    .EntityReference?: void {
        .Value: any
        .Node*: XMLNodeContent
    }
    .Notation?: void {
        .Value: any
    }
    .Attribute?: void {
        .Name: any
        .Value: any
        .Prefix: any
        .Namespace: any
    }
}

type XMLNodes: void {
    .Node*: XMLNodeContent
}

type AddToRequest: void {
    .from: XMLNodeELement
    .to: void {
        .resourceName: string
        .path: string
        .index: int
    }
}

type CreatePlainXMLRequest: void {
    .resourceName: string
    .xml: string
}

type GetElementRequest: void {
    .resourceName: string
    .path: string
}

type ModifyElementRequest: void {
    .resourceName: string
    .path: string
    .content: XMLNodeELement
}

type ShowTreeResponse: void {
    .path*: string
    .tree: undefined
}

type RemoveElementRequest: void {
    .resourceName: string
    .path: string
    .remove_next_text?: bool
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
    getElement( GetElementRequest )( XMLNodeELement ) throws ResourceDoesNotExist,
    getXMLString( ResourceRequest )( string ) throws ResourceDoesNotExist,
    modifyElement( ModifyElementRequest )( void ) throws ResourceDoesNotExist,
    removeElement( RemoveElementRequest )( void ) throws ResourceDoesNotExist,
    showTree( ResourceRequest )( ShowTreeResponse ) throws ResourceDoesNotExist
}
