//include "provaInputPorts.iol"

type BalintType:void{
.b:int
}

type TwiceReqMsgType:void {
.num:void{
.in:double
}
}


type TwiceRespMsgType:void {
.result:BalintType
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