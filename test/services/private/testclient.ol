include "DEMOInterface.iol"

interface DEMOInterfaceHTTP {
RequestResponse:
	getOrdersByItem,
	putOrder,
	getOrders,
	getUsers,
	deleteOrder
}

execution{ concurrent }

outputPort DEMOPort{
Location: "socket://localhost:8000/"
Protocol: http{
.format = "json";.responseHeaders="@header";.osc.getOrdersByItem.alias="/getOrdersByItem";
.osc.getOrdersByItem.method="post";
.osc.putOrder.alias="/putOrder";
.osc.putOrder.method="put";
.osc.getOrders.alias="/orders/%!{userId}?maxItems=%!{maxItems}";
.osc.getOrders.method="get";
.osc.getUsers.alias="users/%!{country}";
.osc.getUsers.method="post";
.osc.deleteOrder.alias="/deleteOrder";
.osc.deleteOrder.method="delete"}
Interfaces: DEMOInterfaceHTTP
}

inputPort DEMO{
Location:"local"
Protocol: sodep
Interfaces: DEMOInterface
}

init { install( default => nullProcess ) }
main {
	[ getOrdersByItem( request )( response ) {
		getOrdersByItem@DEMOPort( request )( response )
		if ( response.("@header").statusCode == 500) {
 			if ( response.fault == "FaultTest") {
				throw( FaultTest, response.content)
			}
			if ( response.fault == "FaultTest2") {
				throw( FaultTest2, response.content)
			}
		}
		undef( response.("@header"))
	}]

	[ putOrder( request )( response ) {
		putOrder@DEMOPort( request )( response )
		if ( response.("@header").statusCode == 500) {
 			throw( FaultTest3, response.content)
		}
		undef( response.("@header"))
	}]

	[ getOrders( request )( response ) {
		getOrders@DEMOPort( request )( response )
		if ( response.("@header").statusCode == 500) {
 			throw( Fault500,"Internal Server Error")
		}
		if ( response.("@header").statusCode == 404) {
 			throw( Fault404,"resource not found")
		}
		undef( response.("@header"))
	}]

	[ getUsers( request )( response ) {
		getUsers@DEMOPort( request )( response )
		if ( response.("@header").statusCode == 500) {
 			throw( Fault500,"Internal Server Error")
		}
		if ( response.("@header").statusCode == 404) {
 			throw( Fault404,"resource not found")
		}
		undef( response.("@header"))
	}]

	[ deleteOrder( request )( response ) {
		deleteOrder@DEMOPort( request )( response )
		if ( response.("@header").statusCode == 500) {
 			throw( Fault500,"Internal Server Error")
		}
		if ( response.("@header").statusCode == 404) {
 			throw( Fault404,"resource not found")
		}
		undef( response.("@header"))
	}]

}