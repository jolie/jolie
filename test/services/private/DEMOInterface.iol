type OrderItem:void {
  .quantity[1,1]:int
  .price[1,1]:double
  .name[1,1]:string
}

type Order:void {
  .date[1,1]:string
  .id[0,1]:int
  .title[1,1]:string
  .items[0,*]:OrderItem
}

type PutOrderRequest:void {
  .userId[1,1]:string
  .order[1,1]:Order
}

type PutOrderResponse:void

type FaultTest2:void {
  .fieldfult1[1,1]:string
}

type GetUsersRequest:void {
  .country[1,1]:string
  .city[1,1]:string
  .surname[1,1]:string
}

type GetUsersResponse:void {
  .users[0,*]:void {
    .country[1,1]:string
    .city[1,1]:string
    .surname[1,1]:string
    .name[1,1]:string
  }
}

type GetOrdersByItemRequest:void {
  .itemName[1,1]:string
  .quantity[1,1]:int
  .userId[1,1]:string
}|void {
  .itemName[1,1]:string
  .userId[1,1]:string
}|void {
  .userId[1,1]:string
}

type Orders:void {
  .orders[0,*]:Order
}

type GetOrdersByItemResponse:Orders

type FaultTest1:void {
  .fieldfault1[1,1]:string
  .fieldfault2[1,1]:int
}

type DeleteOrderRequestLinked:void {
  .orderId[1,1]:int
}

type DeleteOrderRequest:DeleteOrderRequestLinked

type DeleteOrderResponse:void

type GetOrdersRequest:void {
  .maxItems[1,1]:int
  .userId[1,1]:string
}

type GetOrdersResponse:Orders

interface DEMOInterface {
RequestResponse:
  putOrder( PutOrderRequest )( PutOrderResponse ) throws FaultTest3(FaultTest2)  ,
  getUsers( GetUsersRequest )( GetUsersResponse ),
  getOrdersByItem( GetOrdersByItemRequest )( GetOrdersByItemResponse ) throws FaultTest2 FaultTest(FaultTest1)  ,
  deleteOrder( DeleteOrderRequest )( DeleteOrderResponse ),
  getOrders( GetOrdersRequest )( GetOrdersResponse )
}

