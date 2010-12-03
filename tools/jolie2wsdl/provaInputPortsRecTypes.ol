//include "provaInputPorts.iol"

type BalintType:void {
.balint:string
}
type FranzType:void {
.franz:string
}

type TwiceReqMsgType:void {
.num:int
.c2:BalintType
}

type TwiceRespMsgType:void {
.result:int
}

interface MyMathInterface {

RequestResponse:
	twice(TwiceReqMsgType)(TwiceRespMsgType)
}

//------------------------------ .ol

inputPort MathServiceInPort {
Location: "socket://localhost:8000"
Protocol: soap
RequestResponse:twice
Interfaces:MyMathInterface
}


main
{
	twice(req)(resp) {
            resp=req
	}
}