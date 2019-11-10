type Order: void {
	.date:string
	.id[0,1]:int
	.title:string
	.items[0,*]:OrderItem
}
type PutOrderResponse: void
type Orders: void {
	.orders[0,*]:Order
}
type GetUsersResponse: void {
	.users[0,*]:undefined
}
type PutOrderRequest: void {
	.userId:string
	.order:Order
}
type GetUsersRequest: void {
	.country:string
	.city:string
	.surname:string
}
type FaultTest2: void {
	.fieldfult1:string
}
type FaultTest1: void {
	.fieldfault1:string
	.fieldfault2:int
}
type GetOrdersRequest: void {
	.maxItems:int
	.userId:string
}
type JolieFaultType2: void {
	.fault:string
	.content:FaultTest1
}
type JolieFaultType1: void {
	.fault:string
	.content:void
}
type OrderItem: void {
	.quantity:int
	.price:double
	.name:string
}
type GetOrdersByItemRequest:void {
	.itemName:string
	.quantity:int
	.userId:string
}
| void {
	.itemName:string
	.userId:string
}
| void {
	.userId:string
}
type GetOrdersByItemResponse: Orders
type DeleteOrderRequestLinked: void {
	.orderId:int
}
type JolieFaultType0: void {
	.fault:string
	.content:FaultTest2
}
type DeleteOrderRequest: DeleteOrderRequestLinked
type DeleteOrderResponse: void
type GetOrdersResponse: Orders
type getOrdersByItemRequest: GetOrdersByItemRequest
type getOrdersByItemResponse:GetOrdersByItemResponse
type putOrderRequest: PutOrderRequest
type putOrderResponse:PutOrderResponse
type getUsersRequest: void {
.country:string
.city: undefined
.surname: undefined
}
type getUsersResponse:GetUsersResponse
type deleteOrderRequest: DeleteOrderRequest
type deleteOrderResponse:DeleteOrderResponse
type getOrdersRequest: void {
.maxItems:int
.userId:string
}
type getOrdersResponse:GetOrdersResponse
interface DEMOInterface{
RequestResponse:
	getOrdersByItem( getOrdersByItemRequest )( getOrdersByItemResponse ) throws  FaultTest(FaultTest1) Fault500_1(JolieFaultType1)  Fault404( string ) ,
	putOrder( putOrderRequest )( putOrderResponse ) throws FaultTest3(FaultTest2) Fault404( string ) ,
	getUsers( getUsersRequest )( getUsersResponse ) throws Fault500( string )  Fault404( string ) ,
	deleteOrder( deleteOrderRequest )( deleteOrderResponse ) throws Fault500( string )  Fault404( string ) ,
	getOrders( getOrdersRequest )( getOrdersResponse ) throws Fault500( string )  Fault404( string ) 
}

