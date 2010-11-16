//include "provaInputPorts.iol"

type TwiceReqMsgType:void {
.num:int
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
Protocol: sodep
RequestResponse:twice
Interfaces:MyMathInterface
}


main
{
	twice(req)(resp) {
            resp=req
	}
}