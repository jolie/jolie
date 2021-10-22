from .http_template_interface import HttpTemplateInterface
from string_utils import StringUtils
from console import Console

service HttpTemplateServer{

    inputPort TestHttpTemplate {
        interfaces: HttpTemplateInterface
        protocol: "http"{
           .compression= false
           .osc.getOrder.template = "/api/orders/{id}" 
           .osc.getOrder.method = "GET"
           .osc.getOrder.inHeaders.Authorization = "token"
           .osc.getOrders.template = "/api/orders" 
           .osc.getOrders.method = "GET"
           .osc.getOrders.inHeaders.Authorization = "token"
           .osc.addOrder.template="/api/orders" 
           .osc.addOrder.method="POST"
           .osc.addOrder.inHeaders.Authorization = "token"
        }
        location : "socket://localhost:9099"
    }
    execution: concurrent
    
    embed StringUtils as stringUtils
    embed Console as console 
    
    main{
        [getOrder(request)(response){
           response.id = request.id
           response.ammount = global.orders.(request.id).ammount
        }]
        [getOrders(request)(response){
            foreach (orderId : global.orders ){
                response.orders[#response.orders]<< {id = orderId
                                                     ammount = global.orders.(orderId).ammount }
            }
        }]
        [addOrder(request)(response){
            getRandomUUID@stringUtils(  )( orderId )
            global.orders.(orderId).ammount = request.ammount
        
        }]
    }
}




