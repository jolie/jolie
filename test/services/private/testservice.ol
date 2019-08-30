

type OrderItem: void {
    .name: string
    .quantity: int
    .price: double
}

type Order: void {
    .title: string
    .id?: int
    .date: string
    .items*: OrderItem
}

type Orders: void {
    .orders*: Order
}

type GetOrdersRequest: void {
    .userId: string
    .maxItems: int
}

type GetOrdersResponse: Orders

type GetOrdersByItemRequest: void {
    .userId: string
    .itemName: string
    .quantity: int
}
|
void {
    .userId: string
    .itemName: string
}
|
void {
    .userId: string
}

type GetOrdersByItemResponse: Orders

type PutOrderRequest: void {
    .userId: string
    .order: Order
}

type PutOrderResponse: void

type DeleteOrderRequestLinked: void {
    .orderId: int
}

type DeleteOrderRequest: DeleteOrderRequestLinked

type DeleteOrderResponse: void

type FaultTestType: void {
    .message: string
    .code: int
}


interface DemoInterface {
  RequestResponse:
    getOrders( GetOrdersRequest )( GetOrdersResponse ),
    getOrdersByIItem( GetOrdersByItemRequest )( GetOrdersByItemResponse ) throws FaultTest( FaultTestType ),
    putOrder( PutOrderRequest )( PutOrderResponse ),
    deleteOrder( DeleteOrderRequest )( DeleteOrderResponse )
}

execution{ concurrent }
inputPort DEMO {
  Location: "local"
  Protocol: sodep
  Interfaces: DemoInterface
}

init {
      with( global.orders[ 0 ] ) {
        .title = "order0";
        .id = 1;
        .date = "01/01/2001";
        with( .items[ 0 ] ) {
          .name = "itemA";
          .quantity = 2;
          .price = 10.8
        };
        with( .items[ 1 ] ) {
          .name = "itemb";
          .quantity = 3;
          .price = 11.8
        };
        with( .items[ 2 ] ) {
          .name = "itemC";
          .quantity = 3;
          .price = 15.0
        }
      }
      ;
      with( global.orders[ 1 ] ) {
        .title = "order1";
        .id = 2;
        .date = "02/02/2002";
        with( .items[ 0 ] ) {
          .name = "itemA";
          .quantity = 2;
          .price = 10.8
        };
        with( .items[ 1 ] ) {
          .name = "itemb";
          .quantity = 3;
          .price = 11.8
        };
        with( .items[ 2 ] ) {
          .name = "itemC";
          .quantity = 3;
          .price = 15.0
        }
      }
      ;
      with( global.orders[ 2 ] ) {
        .title = "order2";
        .id = 2;
        .date = "03/03/2003";
        with( .items[ 0 ] ) {
          .name = "itemA";
          .quantity = 2;
          .price = 10.8
        };
        with( .items[ 1 ] ) {
          .name = "itemb";
          .quantity = 3;
          .price = 11.8
        };
        with( .items[ 2 ] ) {
          .name = "itemC";
          .quantity = 3;
          .price = 15.0
        }
      }
}

main {
  [ getOrders( request )( response ) {
      response.orders -> global.orders
  }]

  [ getOrdersByIItem( request )( response ) {
      if ( request.quantity > 1 ) {
         f.message = "test message"
         f.code = 100
         throw( FaultTest, f )
      }
      response.orders -> global.orders
  }]

  [ putOrder( request )( response ) {
      orders_max = #orders;
      with( orders[ orders_max ] ) {
        .title = request.title;
        .id = orders_max;
        .date = request.date;
        for( i = 0, i < #request.items, i++ ) {
            with( .items[ i ] ) {
              .name = request.items[ i ].name;
              .quantity = request.items[ i ].quantity;
              .price = request.items[ i ].price
            }
        }
      }
  }]

  [ deleteOrder( request )( response ) {
      nullProcess
  }]
}
