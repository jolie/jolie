include "../AbstractTestUnit.iol"
include "monitors/standard_monitor.iol"
include "runtime.iol"


define doTest
{
    setMonitor@Runtime( Monitor )()

    flush@Monitor()()
}