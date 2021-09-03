from .http_template_interface import HttpTemplateInterface
from string_utils import StringUtils

service HttpTemplateServer{

    inputPort TestHttpTemplate {
        interfaces: HttpTemplateInterface
        protocol: "http"{
           .osc.getOrder.template = "/api/orders/{id}" 
           .osc.getOrder.method = "GET"
           .osc.getOrder.inHeaders.Authorization = "token"
           .osc.getOrders.template = "/api/orders" 
           .osc.getOrders.method = "GET"
           .osc.getOrders.outHeaders.Authorization = "token"
           .osc.addOrder.template="/api/orders" 
           .osc.addOrder.method="POST"
           .osc.addOrder.outHeaders.Authorization = "token"
        }
        location : "socket://localhost:9099"
    }
    execution: concurrent
    
    embed StringUtils as stringUtils
    
    main{
        [getOrder(request)(response){
           response.id = request.id
           response.ammount = global.orders.(id).ammount
        }]
        [getOrders(request)(response){
            foreach (orderId : global.orders ){
                response.orders[#response.orders]<< {id = orderId
                                                     ammount = global.orders.(orderId).ammount }
            }
        }]
        [addOrder(request)(response){
            getRandomUUID@stringUtils( request )( orderId )
            global.orders.(orderId).ammount = request.ammount
        }]
    }
}




