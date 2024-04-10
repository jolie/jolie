from .interfaces import ReporterInterface
from console import Console

service SpecReporter {

    embed Console as console

    execution { concurrent }

    inputPort reporterIP {
        Location: "local"
        Interfaces: ReporterInterface
    }

    init {
        global.indents = 0
        global.n = 0
    }

    main {

        [eventRunBegin()() {
            nullProcess
        }]

        [eventTestPass(test)(){
            for ( i = 0, i < global.indents, i++){
                print@console("  ")()
            }
            print@console("  ")()
            println@console("✅ pass " + test.title)()
        }] 

        [eventTestFail(testFailed)(){
            if (testFailed.error == "InvocationFault"){
                error = "InvocationFault: please ensure that operation is available on target test module" 
            } else {
                error = testFailed.error
            }
            for ( i = 0, i < global.indents, i++){
                print@console("  ")()
            }
            print@console("  ")()
            println@console("❌ failed " + testFailed.title + ", " + error)()
        }] 

        [eventServiceBegin(service)(){
            global.indents++
            print@console("  ")()
            println@console(service.title)()
        }] 

        [eventServiceEnd(stats)(){
            print@console("  ")()
            println@console("passes " + stats.passes + " (" + stats.durations + "ms) failures " + stats.failures)()
            global.indents = 0
        }]

        [eventRunEnd()(){
            nullProcess
        }]

    }
    
}