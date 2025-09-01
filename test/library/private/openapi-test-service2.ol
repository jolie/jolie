

type OrderItem: void {
    .name: string
    .quantity: int
    .price: double
}

type Order: void {
    .title: string ( regex( ".*" ) )
    .id?: int
    .date: string ( regex( "[0-3][0-9]/[0-1][0-9]/20[0-9][0-9]|[0-3][0-9]\\.[0-1][0-9]\\.20[0-9][0-9]" ) )
    .items*: OrderItem
}

type Orders: void {
    .orders*: Order
}

type GetOrdersRequest: void {
    maxItems: int ( ranges( [1,*] ) )
}

type GetOrdersResponse: Orders

type GetOrdersByItemRequest: void {
    orderId: int
}


type GetOrdersByItemResponse: Orders

type PutOrderRequest: void {
    order: Order
}

type PutOrderResponse: void

type DeleteOrderRequestLinked: void {
    orderId: int
}

type DeleteOrderRequest: DeleteOrderRequestLinked

type OrderNotFoundType {
    message: string 
    orderId: int
}

type FaultTest2: void {
    .fieldfult1: string
}

type GetUsersRequest: void {
    .city: string
    .surname: string
    .country: string ( enum ( [ "USA", "UK" ] ))
}

type GetUsersResponse: void {
  .users*: void {
    .name: string 
    .surname: string
    .country: string
    .city: string
  }
}

interface DemoInterface {
  RequestResponse:
    getOrders( GetOrdersRequest )( GetOrdersResponse ),
    getOrdersByItem( GetOrdersByItemRequest )( GetOrdersByItemResponse ) throws OrderNotFound( OrderNotFoundType ),
    putOrder( PutOrderRequest )( void ),
    deleteOrder( DeleteOrderRequest )( void ) throws OrderNotFound( OrderNotFoundType ) OrderNotFound2( string ) OrderNotFound3( void )
}

service TestServiceForOpenApi {

  execution: concurrent 
  
  inputPort DEMO {
    location: "local"
    protocol: sodep
    interfaces: DemoInterface
  }

  init {
      global.orders[ 0 ] << {
        title = "order0";
        id = 1
        date = "01/01/2001"
        items[ 0 ] << {
          name = "itemA"
          quantity = 2
          price = 10.8
        }
        items[ 1 ] << {
          name = "itemb"
          quantity = 3
          price = 11.8
        }
        items[ 2 ] << {
          name = "itemC"
          quantity = 3
          price = 15.0
        }
      }
      global.orders[ 1 ] << {
          title = "order1"
          id = 2
          date = "02/02/2002"
          items[ 0 ] << {
            name = "itemA"
            quantity = 2
            price = 10.8
          }
          items[ 1 ] << {
            name = "itemb"
            quantity = 3
            price = 11.8
          }
          items[ 2 ] << {
            name = "itemC";
            quantity = 3;
            price = 15.0
          }
      }
      global.orders[ 2 ] << {
          title = "order2"
          id = 2
          date = "03/03/2003"
          items[ 0 ] << {
            name = "itemA"
            quantity = 2
            price = 10.8
          }
          items[ 1 ] << {
            name = "itemb"
            quantity = 3
            price = 11.8
          }
          items[ 2 ] << {
            name = "itemC"
            quantity = 3
            price = 15.0
          }
      }

      install( default => nullProcess )
    }


  main {
    [ getOrders( request )( response ) {
        response.orders -> global.orders
    }]


    [ getOrdersByItem( request )( response ) {
        if ( request.orderId >= #global.orders ) {
            fault << {
              orderId = request.orderId
              message = "orderId out of range"
            }
            throw( OrderNotFound, fault )
        }
        response.orders -> global.orders[ request.orderId ]
    }]

    [ putOrder( request )( response ) {
        global.orders[ #global.orders ] << request.order
    }]

    [ deleteOrder( request )( response ) {
        if ( is_defined( global.orders[ request.orderId ] ) ) {
            undef( global.orders[ request.orderId ] )
        } else {
            fault << {
              orderId = request.orderId
              message = "orderId out of range"
            }
            throw( OrderNotFound, fault )
        }
    }]

    
  }
}
