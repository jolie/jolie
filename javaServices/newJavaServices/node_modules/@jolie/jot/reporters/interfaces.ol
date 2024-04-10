from ..jot import Stats
type Test {
    title: string
}

type TestFailed {
    title: string
    error: any
}

type Service {
    title: string
}

interface ReporterInterface {
	RequestResponse: 
        eventRunBegin(void)(void),
        eventTestPass(Test)(void),
        eventTestFail(TestFailed)(void),
        eventServiceBegin(Service)(void),
        eventServiceEnd(Stats)(void),
        eventRunEnd(void)(void)
}