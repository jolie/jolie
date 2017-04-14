include "xml_utils.iol"
include "console.iol"
include "string_utils.iol"
include "runtime.iol"
include "PlainXMLManagerInterface.iol"

execution{ concurrent }

type GetNodeInnerRequest: void {
   .tree: undefined
   .path: string
}

interface PlainXMLManagerInnerInterface {
  RequestResponse:
    getNodeInner( GetNodeInnerRequest )( undefined )
}

outputPort MySelf {
  Interfaces: PlainXMLManagerInnerInterface
}

inputPort PlainXMLManager {
  Location: "local"
  Interfaces: PlainXMLManagerInterface, PlainXMLManagerInnerInterface
}

init {
  getLocalLocation@Runtime()( MySelf.location )
}

define __recompose_path {
    __path = "";
    for( i = __from, i < #spl_res.result, i++ ) {
        __path = __path + "/" + spl_res.result[ i ]
    }
}



define _showTree {
  stack[ #stack ] =$ x;

  foreach( y : x ) {
          for( z = 0, z < #x.( y ), z++ ) {
              if ( y == "Element" ) {
                  path_tmp = "";
                  for( i = 0, i < #path, i++ ) {
                     path_tmp = path_tmp + path[ i ]
                  };
                  path_tmp = path_tmp + x.( y ).Name;
                  _path[ #_path ] = path_tmp;
                  path[ #path ] = x.( y ).Name + "/"
              };

              mv = #v;
              with( v[ mv ] ) {
                  .z = z;
                  .y = y
              };
              x =$ x.( y )[ z ];

              _showTree;
              z = v[ #v - 1 ].z;
              y = v[ #v - 1 ].y;
              undef( v[ #v - 1 ] );
              if ( y == "Element" ) {
                  undef( path[ #path -1 ] )
              }
          }
  };

  if ( #stack > 1 ) {
    x =$ stack[ #stack - 2 ]
  };
  undef( stack[ #stack - 1 ] )
}

main {

   /* public */
    [ addTo( request )( response ) {
      if ( !is_defined( global.resources.( request.from.resourceName ) ) ) {
        throw( ResourceAlreadyExists, request.from.resourceName )
      };
      if ( !is_defined( global.resources.( request.to.resourceName ) ) ) {
        throw( ResourceAlreadyExists, request.to.resourceName )
      }
      /* TODO */
    }]

    [ createPlainXML( request )( response ) {
        if ( is_defined( global.resources.( request.resourceName ) ) ) {
          throw( ResourceAlreadyExists )
        } else {
          synchronized( write ) {
            xmlToPlainValue@XmlUtils( request.xml )( global.resources.( request.resourceName ) )
          }
        }
    }]

    [ destroyPlainXML( request )( response ) {
        if ( !is_defined( global.resources.( request.resourceName ) ) ) {
          throw ( ResourceDoesNotExist )
        } else {
          synchronized( write ) {
            undef( global.resources.( request.resourceName ) )
          }
        }
    }]

    [ getNode( request )( response ) {
        if ( !is_defined( global.resources.( request.resourceName ) ) ) {
          throw ( ResourceDoesNotExist )
        } else {
          spl = request.path;
          spl.regex = "/";
          split@StringUtils( spl )( spl_res );
          resource -> global.resources.( request.resourceName ).root;
          for( n = 0, n < #resource.Node, n++ ) {
              if ( is_defined( resource.Node[ n ].Element ) ) {
                  if ( resource.Node[ n ].Element.Name == spl_res.result[ 2 ] ) {  //we skip the root
                      __from = 3;
                      __recompose_path;
                      if ( __path != "" ) {
                          with( req ) {
                              .tree << resource.Node[ n ].Element;
                              .path = __path
                          };
                          getNodeInner@MySelf( req )( response )
                      } else {
                          response << resource.Node[ n ].Element
                      }
                  }
              }
          }
        }
    }]

    [ getNodeInner( request )( response ) {
        spl = request.path;
        spl.regex = "/";
        split@StringUtils( spl )( spl_res );
        for( n = 0, n < #request.tree.Node, n++ ) {
            if ( is_defined( request.tree.Node[ n ].Element ) ) {
                if ( request.tree.Node[ n ].Element.Name == spl_res.result[ 1 ] ) {
                    __from = 2;
                    __recompose_path;
                    if ( __path != "" ) {
                        with( req ) {
                            .tree << request.tree.Node[ n ].Element;
                            .path = __path
                        };
                        getNodeInner@MySelf( req )( response )
                    } else {
                        response << request.tree.Node[ n ].Element
                    }
                }
            }
        }
    }]

    [ getXMLString( request )( response ) {
        if ( !is_defined( global.resources.( request.resourceName ) ) ) {
          throw ( ResourceDoesNotExist )
        } else {
          plainValueToXml@XmlUtils( global.resources.( request.resourceName ) )( response )
        }
    }]

    [ showTree( request )( response ) {
      if ( !is_defined( global.resources.( request.resourceName ) ) ) {
        throw ( ResourceDoesNotExist )
      } else {
         x =$ global.resources.( request.resourceName ).root;
         _showTree;
         response.path -> _path
      }
    }]
}
