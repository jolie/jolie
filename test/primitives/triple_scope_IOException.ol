include "exec.iol"
include "string_utils.iol"
include "console.iol"
include "file.iol"

include "../AbstractTestUnit.iol"

define doTest {
	getFileSeparator@File()( fs )

	req = "jolie"
	;
	with(req) {
		.args[0] = "triple_scope_IOException2.ol"; 
		.workingDirectory= "." + fs + "primitives" + fs + "unhandled_faults" + fs;
		.stdOutConsoleEnable = true;
		.waitFor = 1
	}
	;
	exec@Exec(req)(res)
	;
	valueToPrettyString@StringUtils(res)(s);
	
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
