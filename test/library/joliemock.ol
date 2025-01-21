from ..test-unit import TestUnitInterface
from joliemock	import JolieMock
from metajolie import MetaJolie
from console import Console 
from message-digest import MessageDigest
from runtime import Runtime


service Main {

	embed MetaJolie as MetaJolie
	embed JolieMock as JolieMock
	embed MessageDigest as MessageDigest
	embed Console as Console
	embed Runtime as Runtime

	inputPort TestUnitInput {
        location: "local"
        interfaces: TestUnitInterface
    }

	main {
		test()() {
			getInputPortMetaData@MetaJolie( { filename = "./services/private/testservice.ol" } )( ipts )
			getMock@JolieMock( ipts )( mock )
			// just testing if the code is syntactically correct
			loadEmbeddedService@Runtime({ code = mock })()
			
			md5@MessageDigest( mock )( mockmd5 )
			if ( mockmd5 != "53e25302c100d06d1f0d15addbae931a" ) {
				throw( TestFailed, "md5 of mock does not correspond, expected 663c50e2d6cd7901c1aa5e6d4dfb32b2, found " + mockmd5 )
			}
		}
	}
}

/* expected mock
type DeleteOrderRequest:DeleteOrderRequestLinked

type DeleteOrderRequestLinked:void {
  .orderId[1,1]:int
}

type DeleteOrderResponse:void

type FaultTest1:void {
  .fieldfault1[1,1]:string
  .fieldfault2[1,1]:int
}

type FaultTest2:void {
  .fieldfult1[1,1]:string
}

type GetOrdersByItemRequest:void {
  .itemName[1,1]:string
  .quantity[1,1]:int
  .userId[1,1]:string( length( [ 1,50 ] ) )
  }|void {
  .itemName[1,1]:string
  .userId[1,1]:string( length( [ 1,50 ] ) )
}|void {
  .userId[1,1]:string( length( [ 1,50 ] ) )
}}

type GetOrdersByItemResponse:Orders

type GetOrdersRequest:void {
  .maxItems[1,1]:int( ranges( [1,*]) )
  .userId[1,1]:string( length( [ 1,50 ] ) )
}

type GetOrdersResponse:Orders

type GetUsersRequest:void {
  .country[1,1]:string( enum(["USA","UK" ] ) )
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

type Order:void {
  .date[1,1]:string( regex( "[0-3][0-9]/[0-1][0-9]/20[0-9][0-9]|[0-3][0-9]\\.[0-1][0-9]\\.20[0-9][0-9]" ) )
  .id[0,1]:int
  .title[1,1]:string( regex( ".*" ) )
  .items[0,*]:OrderItem
}

type OrderItem:void {
  .quantity[1,1]:int
  .price[1,1]:double
  .name[1,1]:string
}

type Orders:void {
  .orders[0,*]:Order
}

type PutOrderRequest:void {
  .userId[1,1]:string( length( [ 1,50 ] ) )
  .order[1,1]:Order
}

type PutOrderResponse:void

interface DEMOInterface {
RequestResponse:
  deleteOrder( DeleteOrderRequest )( DeleteOrderResponse ),
  getOrders( GetOrdersRequest )( GetOrdersResponse ),
  getOrdersByItem( GetOrdersByItemRequest )( GetOrdersByItemResponse ) throws FaultTest2 FaultTest(FaultTest1)  ,
  getUsers( GetUsersRequest )( GetUsersResponse ),
  putOrder( PutOrderRequest )( PutOrderResponse ) throws FaultTest3(FaultTest2)
}



include "console.iol"
include "string_utils.iol"
include "converter.iol"

execution{ concurrent }

inputPort DEMO {
  Protocol:sodep
  Interfaces:DemoInterface
}



        init {
            STRING_CONST = "mock_string"
            INT_CONST = 42
            DOUBLE_CONST = 42.42
            stringToRaw@Converter("hello")( RAW_CONST )
            ANY_CONST = "mock any"
            BOOL_CONST = true
            LONG_CONST = 42L
            VOID_CONST = Void
            println@Console("Mock service is running...")()
        }


        

main {
[ deleteOrder( request )( response ) {
	valueToPrettyString@StringUtils( request )( s ); println@Console( s )()
	response = VOID_CONST
}]

[ getOrders( request )( response ) {
	valueToPrettyString@StringUtils( request )( s ); println@Console( s )()
	response = VOID_CONST
	response.orders[ 0 ] = VOID_CONST
	response.orders[ 0 ].date[ 0 ] = "response.orders[ 0 ].date[ 0 ]"
	response.orders[ 0 ].id[ 0 ] = 29
	response.orders[ 0 ].title[ 0 ] = "response.orders[ 0 ].title[ 0 ]"
	response.orders[ 0 ].items[ 0 ] = VOID_CONST
	response.orders[ 0 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 0 ].name[ 0 ] = "response.orders[ 0 ].items[ 0 ].name[ 0 ]"
	response.orders[ 0 ].items[ 1 ] = VOID_CONST
	response.orders[ 0 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 1 ].name[ 0 ] = "response.orders[ 0 ].items[ 1 ].name[ 0 ]"
	response.orders[ 0 ].items[ 2 ] = VOID_CONST
	response.orders[ 0 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 2 ].name[ 0 ] = "response.orders[ 0 ].items[ 2 ].name[ 0 ]"
	response.orders[ 0 ].items[ 3 ] = VOID_CONST
	response.orders[ 0 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 3 ].name[ 0 ] = "response.orders[ 0 ].items[ 3 ].name[ 0 ]"
	response.orders[ 0 ].items[ 4 ] = VOID_CONST
	response.orders[ 0 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 4 ].name[ 0 ] = "response.orders[ 0 ].items[ 4 ].name[ 0 ]"
	response.orders[ 1 ] = VOID_CONST
	response.orders[ 1 ].date[ 0 ] = "response.orders[ 1 ].date[ 0 ]"
	response.orders[ 1 ].id[ 0 ] = 29
	response.orders[ 1 ].title[ 0 ] = "response.orders[ 1 ].title[ 0 ]"
	response.orders[ 1 ].items[ 0 ] = VOID_CONST
	response.orders[ 1 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 0 ].name[ 0 ] = "response.orders[ 1 ].items[ 0 ].name[ 0 ]"
	response.orders[ 1 ].items[ 1 ] = VOID_CONST
	response.orders[ 1 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 1 ].name[ 0 ] = "response.orders[ 1 ].items[ 1 ].name[ 0 ]"
	response.orders[ 1 ].items[ 2 ] = VOID_CONST
	response.orders[ 1 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 2 ].name[ 0 ] = "response.orders[ 1 ].items[ 2 ].name[ 0 ]"
	response.orders[ 1 ].items[ 3 ] = VOID_CONST
	response.orders[ 1 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 3 ].name[ 0 ] = "response.orders[ 1 ].items[ 3 ].name[ 0 ]"
	response.orders[ 1 ].items[ 4 ] = VOID_CONST
	response.orders[ 1 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 4 ].name[ 0 ] = "response.orders[ 1 ].items[ 4 ].name[ 0 ]"
	response.orders[ 2 ] = VOID_CONST
	response.orders[ 2 ].date[ 0 ] = "response.orders[ 2 ].date[ 0 ]"
	response.orders[ 2 ].id[ 0 ] = 29
	response.orders[ 2 ].title[ 0 ] = "response.orders[ 2 ].title[ 0 ]"
	response.orders[ 2 ].items[ 0 ] = VOID_CONST
	response.orders[ 2 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 0 ].name[ 0 ] = "response.orders[ 2 ].items[ 0 ].name[ 0 ]"
	response.orders[ 2 ].items[ 1 ] = VOID_CONST
	response.orders[ 2 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 1 ].name[ 0 ] = "response.orders[ 2 ].items[ 1 ].name[ 0 ]"
	response.orders[ 2 ].items[ 2 ] = VOID_CONST
	response.orders[ 2 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 2 ].name[ 0 ] = "response.orders[ 2 ].items[ 2 ].name[ 0 ]"
	response.orders[ 2 ].items[ 3 ] = VOID_CONST
	response.orders[ 2 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 3 ].name[ 0 ] = "response.orders[ 2 ].items[ 3 ].name[ 0 ]"
	response.orders[ 2 ].items[ 4 ] = VOID_CONST
	response.orders[ 2 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 4 ].name[ 0 ] = "response.orders[ 2 ].items[ 4 ].name[ 0 ]"
	response.orders[ 3 ] = VOID_CONST
	response.orders[ 3 ].date[ 0 ] = "response.orders[ 3 ].date[ 0 ]"
	response.orders[ 3 ].id[ 0 ] = 29
	response.orders[ 3 ].title[ 0 ] = "response.orders[ 3 ].title[ 0 ]"
	response.orders[ 3 ].items[ 0 ] = VOID_CONST
	response.orders[ 3 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 0 ].name[ 0 ] = "response.orders[ 3 ].items[ 0 ].name[ 0 ]"
	response.orders[ 3 ].items[ 1 ] = VOID_CONST
	response.orders[ 3 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 1 ].name[ 0 ] = "response.orders[ 3 ].items[ 1 ].name[ 0 ]"
	response.orders[ 3 ].items[ 2 ] = VOID_CONST
	response.orders[ 3 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 2 ].name[ 0 ] = "response.orders[ 3 ].items[ 2 ].name[ 0 ]"
	response.orders[ 3 ].items[ 3 ] = VOID_CONST
	response.orders[ 3 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 3 ].name[ 0 ] = "response.orders[ 3 ].items[ 3 ].name[ 0 ]"
	response.orders[ 3 ].items[ 4 ] = VOID_CONST
	response.orders[ 3 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 4 ].name[ 0 ] = "response.orders[ 3 ].items[ 4 ].name[ 0 ]"
	response.orders[ 4 ] = VOID_CONST
	response.orders[ 4 ].date[ 0 ] = "response.orders[ 4 ].date[ 0 ]"
	response.orders[ 4 ].id[ 0 ] = 29
	response.orders[ 4 ].title[ 0 ] = "response.orders[ 4 ].title[ 0 ]"
	response.orders[ 4 ].items[ 0 ] = VOID_CONST
	response.orders[ 4 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 0 ].name[ 0 ] = "response.orders[ 4 ].items[ 0 ].name[ 0 ]"
	response.orders[ 4 ].items[ 1 ] = VOID_CONST
	response.orders[ 4 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 1 ].name[ 0 ] = "response.orders[ 4 ].items[ 1 ].name[ 0 ]"
	response.orders[ 4 ].items[ 2 ] = VOID_CONST
	response.orders[ 4 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 2 ].name[ 0 ] = "response.orders[ 4 ].items[ 2 ].name[ 0 ]"
	response.orders[ 4 ].items[ 3 ] = VOID_CONST
	response.orders[ 4 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 3 ].name[ 0 ] = "response.orders[ 4 ].items[ 3 ].name[ 0 ]"
	response.orders[ 4 ].items[ 4 ] = VOID_CONST
	response.orders[ 4 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 4 ].name[ 0 ] = "response.orders[ 4 ].items[ 4 ].name[ 0 ]"
}]

[ getOrdersByItem( request )( response ) {
	valueToPrettyString@StringUtils( request )( s ); println@Console( s )()
	response = VOID_CONST
	response.orders[ 0 ] = VOID_CONST
	response.orders[ 0 ].date[ 0 ] = "response.orders[ 0 ].date[ 0 ]"
	response.orders[ 0 ].id[ 0 ] = 29
	response.orders[ 0 ].title[ 0 ] = "response.orders[ 0 ].title[ 0 ]"
	response.orders[ 0 ].items[ 0 ] = VOID_CONST
	response.orders[ 0 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 0 ].name[ 0 ] = "response.orders[ 0 ].items[ 0 ].name[ 0 ]"
	response.orders[ 0 ].items[ 1 ] = VOID_CONST
	response.orders[ 0 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 1 ].name[ 0 ] = "response.orders[ 0 ].items[ 1 ].name[ 0 ]"
	response.orders[ 0 ].items[ 2 ] = VOID_CONST
	response.orders[ 0 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 2 ].name[ 0 ] = "response.orders[ 0 ].items[ 2 ].name[ 0 ]"
	response.orders[ 0 ].items[ 3 ] = VOID_CONST
	response.orders[ 0 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 3 ].name[ 0 ] = "response.orders[ 0 ].items[ 3 ].name[ 0 ]"
	response.orders[ 0 ].items[ 4 ] = VOID_CONST
	response.orders[ 0 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 0 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 0 ].items[ 4 ].name[ 0 ] = "response.orders[ 0 ].items[ 4 ].name[ 0 ]"
	response.orders[ 1 ] = VOID_CONST
	response.orders[ 1 ].date[ 0 ] = "response.orders[ 1 ].date[ 0 ]"
	response.orders[ 1 ].id[ 0 ] = 29
	response.orders[ 1 ].title[ 0 ] = "response.orders[ 1 ].title[ 0 ]"
	response.orders[ 1 ].items[ 0 ] = VOID_CONST
	response.orders[ 1 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 0 ].name[ 0 ] = "response.orders[ 1 ].items[ 0 ].name[ 0 ]"
	response.orders[ 1 ].items[ 1 ] = VOID_CONST
	response.orders[ 1 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 1 ].name[ 0 ] = "response.orders[ 1 ].items[ 1 ].name[ 0 ]"
	response.orders[ 1 ].items[ 2 ] = VOID_CONST
	response.orders[ 1 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 2 ].name[ 0 ] = "response.orders[ 1 ].items[ 2 ].name[ 0 ]"
	response.orders[ 1 ].items[ 3 ] = VOID_CONST
	response.orders[ 1 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 3 ].name[ 0 ] = "response.orders[ 1 ].items[ 3 ].name[ 0 ]"
	response.orders[ 1 ].items[ 4 ] = VOID_CONST
	response.orders[ 1 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 1 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 1 ].items[ 4 ].name[ 0 ] = "response.orders[ 1 ].items[ 4 ].name[ 0 ]"
	response.orders[ 2 ] = VOID_CONST
	response.orders[ 2 ].date[ 0 ] = "response.orders[ 2 ].date[ 0 ]"
	response.orders[ 2 ].id[ 0 ] = 29
	response.orders[ 2 ].title[ 0 ] = "response.orders[ 2 ].title[ 0 ]"
	response.orders[ 2 ].items[ 0 ] = VOID_CONST
	response.orders[ 2 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 0 ].name[ 0 ] = "response.orders[ 2 ].items[ 0 ].name[ 0 ]"
	response.orders[ 2 ].items[ 1 ] = VOID_CONST
	response.orders[ 2 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 1 ].name[ 0 ] = "response.orders[ 2 ].items[ 1 ].name[ 0 ]"
	response.orders[ 2 ].items[ 2 ] = VOID_CONST
	response.orders[ 2 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 2 ].name[ 0 ] = "response.orders[ 2 ].items[ 2 ].name[ 0 ]"
	response.orders[ 2 ].items[ 3 ] = VOID_CONST
	response.orders[ 2 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 3 ].name[ 0 ] = "response.orders[ 2 ].items[ 3 ].name[ 0 ]"
	response.orders[ 2 ].items[ 4 ] = VOID_CONST
	response.orders[ 2 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 2 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 2 ].items[ 4 ].name[ 0 ] = "response.orders[ 2 ].items[ 4 ].name[ 0 ]"
	response.orders[ 3 ] = VOID_CONST
	response.orders[ 3 ].date[ 0 ] = "response.orders[ 3 ].date[ 0 ]"
	response.orders[ 3 ].id[ 0 ] = 29
	response.orders[ 3 ].title[ 0 ] = "response.orders[ 3 ].title[ 0 ]"
	response.orders[ 3 ].items[ 0 ] = VOID_CONST
	response.orders[ 3 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 0 ].name[ 0 ] = "response.orders[ 3 ].items[ 0 ].name[ 0 ]"
	response.orders[ 3 ].items[ 1 ] = VOID_CONST
	response.orders[ 3 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 1 ].name[ 0 ] = "response.orders[ 3 ].items[ 1 ].name[ 0 ]"
	response.orders[ 3 ].items[ 2 ] = VOID_CONST
	response.orders[ 3 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 2 ].name[ 0 ] = "response.orders[ 3 ].items[ 2 ].name[ 0 ]"
	response.orders[ 3 ].items[ 3 ] = VOID_CONST
	response.orders[ 3 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 3 ].name[ 0 ] = "response.orders[ 3 ].items[ 3 ].name[ 0 ]"
	response.orders[ 3 ].items[ 4 ] = VOID_CONST
	response.orders[ 3 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 3 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 3 ].items[ 4 ].name[ 0 ] = "response.orders[ 3 ].items[ 4 ].name[ 0 ]"
	response.orders[ 4 ] = VOID_CONST
	response.orders[ 4 ].date[ 0 ] = "response.orders[ 4 ].date[ 0 ]"
	response.orders[ 4 ].id[ 0 ] = 29
	response.orders[ 4 ].title[ 0 ] = "response.orders[ 4 ].title[ 0 ]"
	response.orders[ 4 ].items[ 0 ] = VOID_CONST
	response.orders[ 4 ].items[ 0 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 0 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 0 ].name[ 0 ] = "response.orders[ 4 ].items[ 0 ].name[ 0 ]"
	response.orders[ 4 ].items[ 1 ] = VOID_CONST
	response.orders[ 4 ].items[ 1 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 1 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 1 ].name[ 0 ] = "response.orders[ 4 ].items[ 1 ].name[ 0 ]"
	response.orders[ 4 ].items[ 2 ] = VOID_CONST
	response.orders[ 4 ].items[ 2 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 2 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 2 ].name[ 0 ] = "response.orders[ 4 ].items[ 2 ].name[ 0 ]"
	response.orders[ 4 ].items[ 3 ] = VOID_CONST
	response.orders[ 4 ].items[ 3 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 3 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 3 ].name[ 0 ] = "response.orders[ 4 ].items[ 3 ].name[ 0 ]"
	response.orders[ 4 ].items[ 4 ] = VOID_CONST
	response.orders[ 4 ].items[ 4 ].quantity[ 0 ] = 46
	response.orders[ 4 ].items[ 4 ].price[ 0 ] = 43.0
	response.orders[ 4 ].items[ 4 ].name[ 0 ] = "response.orders[ 4 ].items[ 4 ].name[ 0 ]"
}]

[ getUsers( request )( response ) {
	valueToPrettyString@StringUtils( request )( s ); println@Console( s )()
	response = VOID_CONST
	response.users[ 0 ] = VOID_CONST
	response.users[ 0 ].country[ 0 ] = "response.users[ 0 ].country[ 0 ]"
	response.users[ 0 ].city[ 0 ] = "response.users[ 0 ].city[ 0 ]"
	response.users[ 0 ].surname[ 0 ] = "response.users[ 0 ].surname[ 0 ]"
	response.users[ 0 ].name[ 0 ] = "response.users[ 0 ].name[ 0 ]"
	response.users[ 1 ] = VOID_CONST
	response.users[ 1 ].country[ 0 ] = "response.users[ 1 ].country[ 0 ]"
	response.users[ 1 ].city[ 0 ] = "response.users[ 1 ].city[ 0 ]"
	response.users[ 1 ].surname[ 0 ] = "response.users[ 1 ].surname[ 0 ]"
	response.users[ 1 ].name[ 0 ] = "response.users[ 1 ].name[ 0 ]"
	response.users[ 2 ] = VOID_CONST
	response.users[ 2 ].country[ 0 ] = "response.users[ 2 ].country[ 0 ]"
	response.users[ 2 ].city[ 0 ] = "response.users[ 2 ].city[ 0 ]"
	response.users[ 2 ].surname[ 0 ] = "response.users[ 2 ].surname[ 0 ]"
	response.users[ 2 ].name[ 0 ] = "response.users[ 2 ].name[ 0 ]"
	response.users[ 3 ] = VOID_CONST
	response.users[ 3 ].country[ 0 ] = "response.users[ 3 ].country[ 0 ]"
	response.users[ 3 ].city[ 0 ] = "response.users[ 3 ].city[ 0 ]"
	response.users[ 3 ].surname[ 0 ] = "response.users[ 3 ].surname[ 0 ]"
	response.users[ 3 ].name[ 0 ] = "response.users[ 3 ].name[ 0 ]"
	response.users[ 4 ] = VOID_CONST
	response.users[ 4 ].country[ 0 ] = "response.users[ 4 ].country[ 0 ]"
	response.users[ 4 ].city[ 0 ] = "response.users[ 4 ].city[ 0 ]"
	response.users[ 4 ].surname[ 0 ] = "response.users[ 4 ].surname[ 0 ]"
	response.users[ 4 ].name[ 0 ] = "response.users[ 4 ].name[ 0 ]"
}]

[ putOrder( request )( response ) {
	valueToPrettyString@StringUtils( request )( s ); println@Console( s )()
	response = VOID_CONST
}]


}



*/