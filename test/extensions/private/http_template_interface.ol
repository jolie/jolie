
type Order:void{
    id:string
    ammount:double
}

type GetOrderRequest:void{
    token:string
    id:string 
}

type GetOrderResponse:Order

type GetOrdersRequest:void{
    token:string
}

type GetOrdersResponse:void{
    orders*: Order
}

type AddOrderRequest:void{
    token:string
    ammount:double
}

type AddOrderResponse:void


interface HttpTemplateInterface{
    RequestResponse:

    getOrders(GetOrdersRequest)(GetOrdersResponse),
    getOrder(GetOrderRequest)( GetOrderResponse),
    addOrder(AddOrderRequest)(AddOrderResponse),

}




