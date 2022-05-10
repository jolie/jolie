include "exec.iol"
include "string_utils.iol"
include "console.iol"
include "runtime.iol"

include "../AbstractTestUnit.iol"

define doTest {
	getenv@Runtime("windir")(windir);
	if (windir instanceof void) {
		req = "jolie"
	} else {
		req = "jolie.bat"
	}

	with(req) {
		.args[0] = "init_fault2.ol"; 
		.workingDirectory="./primitives/unhandled_faults/";
		.stdOutConsoleEnable = false;
		.waitFor = 1
	}
	;
	exec@Exec(req)(res)
	;
	valueToPrettyString@StringUtils(res)(s);
	// println@Console(s)()
	undef(req);
	req = s;
	req.substring="Thrown unhandled fault: IOException";
	contains@StringUtils(req)(contain);
	if (!contain){
		throw( TestFailed, "Not an IOException raised" )
	}

	req.substring="java.net.ConnectException";
	contains@StringUtils(req)(contain);
	if (!contain){
		throw( TestFailed, "IOException: not a 'Connection refused' error" )
	}
}
