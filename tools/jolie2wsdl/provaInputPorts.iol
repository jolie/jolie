
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