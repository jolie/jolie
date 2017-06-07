include "xml_utils.iol"
include "console.iol"
include "string_utils.iol"
include "runtime.iol"
include "PlainXMLManagerInterface.iol"

execution{ concurrent }

inputPort PlainXMLManager {
  Location: "local"
  Interfaces: PlainXMLManagerInterface
}


define __navigate {
    /* find node */
    spl = __path;
    spl.regex = "/";
    split@StringUtils( spl )( _nodes_res );
    _node_to_look_for = _nodes_res.result[ 0 ];

    /* find index */
    spl = _node_to_look_for;
    spl.regex = "\\[";
    split@StringUtils( spl )( spl_res );
    if ( #spl_res.result == 1 ) {
        /* no cardinality defined */
        _index = 0
    } else {
        _node_to_look_for = spl_res.result[ 0 ];
        spl = spl_res.result[ 1 ];
        spl.regex = "\\]";
        split@StringUtils( spl )( index_res );
        _index = index_res.result[ 0 ]
    };

    found = false; tmp_index = 0;
    for( n = 0, (n < #__node.Node) && !found, n++ ) {
        if ( is_defined( __node.Node[ n ].Element ) ) {
            if ( (__node.Node[ n ].Element.Name == _node_to_look_for) && ( tmp_index == _index ) ) {  //we skip the root
                found = true;
                __node =$ __node.Node[ n ].Element
            } else if ( (__node.Node[ n ].Element.Name == _node_to_look_for) && ( tmp_index < _index ) ) {
                tmp_index++
            }
        }
    };

    if ( found && __path != "" && __path != "/" ) {
        __path = "";
        for( _t = 1, _t < #_nodes_res.result, _t++ ) {
            __path = __path + _nodes_res.result[ _t ] + "/"
        };
        __navigate
    } else if ( !found && __path != "" && __path != "/" ) {
        undef( __node )
    }
}


define _showTree {
  stack[ #stack ] =$ __x;
  stack_tree[ #stack_tree ] =$ __tree;

  /* range over node sub elements */
  foreach( y : __x ) {
      for( z = 0, z < #__x.( y ), z++ ) {
          if ( y == "Element" ) {
              card = #__tree.( __x.( y ).Name );
              path_tmp = "";
              for( i = 0, i < #path, i++ ) {
                 path_tmp = path_tmp + path[ i ]
              };
              path_tmp = path_tmp + __x.( y ).Name + "[" + card + "]";
              _path[ #_path ] = path_tmp;
              path[ #path ] = __x.( y ).Name + "[" + card + "]/";
              __tree.( __x.( y ).Name )[ card ].__path = path_tmp;
              __tree.( __x.( y ).Name )[ card ].__index = card;
              __tree =$ __tree.( __x.( y ).Name )
          };

          /* moving forward in the tree */
          __mv = #__v;
          with( __v[ __mv ] ) {
              .z = z;
              .y = y
          };
          __x =$ __x.( y )[ z ];
          _showTree;
          z = __v[ #__v - 1 ].z;
          y = __v[ #__v - 1 ].y;
          undef( __v[ #__v - 1 ] );
          if ( y == "Element" ) {
              undef( path[ #path -1 ] )
          }
      }
  };

  if ( #stack > 1 ) {
    __x =$ stack[ #stack - 2 ];
    __tree =$ stack_tree[ #stack_tree - 2 ]
  };
  undef( stack[ #stack - 1 ] );
  undef( stack_tree[ #stack_tree - 1 ] )
}

main {

   /* public */
    [ addElementTo( request )( response ) {
      if ( !is_defined( global.resources.( request.from.resourceName ) ) ) {
        throw( ResourceAlreadyExists, request.from.resourceName )
      };
      if ( !is_defined( global.resources.( request.to.resourceName ) ) ) {
        throw( ResourceAlreadyExists, request.to.resourceName )
      }
      ;
      __node =$ global.resources.( request.to.resourceName ).root;
      __path = request.to.__path;
      __navigate;
      if( __node instanceof void ) {
          throw( PositionOrPathNotAvailable )
      }
      ;
      /* copy extra nodes in temp variable */
      for( n = request.to.__index, n < #__node.Node, n++ ) {
          tmp.Node[ request.to.__index - n ] << __node.Node[ n ]
      };
      /* undef extra nodes */
      for( n = request.to.__index, n < #__node.Node, n++ ) {
          undef( __node.Node[ n ] )
      };
      /* add node */
      __node.Node[ request.to.__index ] <<
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

    [ getElement( request )( response ) {
        if ( !is_defined( global.resources.( request.resourceName ) ) ) {
          throw ( ResourceDoesNotExist )
        } else {
          __node =$ global.resources.( request.resourceName ).root;
          __path = request.__path;
          __navigate;
          response << __node
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
         __x =$ global.resources.( request.resourceName ).root;
         __tree =$ response.tree;
         _showTree;
         response.path -> _path
      }
    }]
}
