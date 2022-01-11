from .private.http_template_interface import HttpTemplateInterface
from .private.http_template_server import HttpTemplateServer
from ..test-unit import TestUnitInterface
<<<<<<< HEAD
from string_utils import StringUtils
from console import Console
=======
>>>>>>> d351ec71e4490f8a6ec54d4d216638fd96debedd

service Test {
	inputPort TestUnitInput {
		location: "local"
		interfaces: TestUnitInterface
	}

<<<<<<< HEAD
    outputPort TestHttpTemplateB{
        Location : "socket://localhost:9099"
    }

    outputPort TestHttpTemplate {
        interfaces: HttpTemplateInterface
        protocol: "http"{
           compression= false 
=======
    outputPort TestHttpTemplate {
        interfaces: HttpTemplateInterface
        protocol: "http"{
>>>>>>> d351ec71e4490f8a6ec54d4d216638fd96debedd
           osc.getOrder.template="/api/orders/{id}" 
           osc.getOrder.method="GET"
           osc.getOrder.outHeaders.("Authorization")= "token"
           osc.getOrders.template="/api/orders" 
           osc.getOrders.method="GET"
           osc.getOrders.outHeaders.("Authorization")= "token"
           osc.addOrder.template="/api/orders" 
           osc.addOrder.method="POST"
           osc.addOrder.outHeaders.("Authorization")= "token"
           osc.addOrder.statusCodes.IOException = 500
        }
        Location : "socket://localhost:9099"
    }

    embed StringUtils as stringUtils
    embed Console as console 
    embed HttpTemplateServer in TestHttpTemplateB
    

	main {
		test()() {
			/*
			* Write the code of your test here (replace nullProcess),
			* and replace the first line of the copyright header with your data.
			*
			* The test is supposed to throw a TestFailed fault in case of a failure.
			* You should add a description that reports what the failure was about,
			* for example:
			*
			* throw( TestFailed, "string concatenation does not match correct result" )
			*/
			addOrder@TestHttpTemplate({token="sometoken" 
                                       ammount = 10.0})()
            addOrder@TestHttpTemplate({token="sometoken" 
                                       ammount = 11.0})()
            addOrder@TestHttpTemplate({token="sometoken" 
                                       ammount = 21.0})()    
            getOrders@TestHttpTemplate({token="sometoken"})(resultGetOrders) 
            if (#resultGetOrders.orders!=3){
                throw( TestFailed, "wrong number of results in getOrders" )
            }    
            request.token = "sometoken"
            request.id = resultGetOrders.orders[2].id
            getOrder@TestHttpTemplate(request)(resultGetOrder) 
        
            if(resultGetOrders.orders[2].id != resultGetOrder.id){
                throw( TestFailed, "wrong id" )
            }                                                                                                         

		}
	}
}