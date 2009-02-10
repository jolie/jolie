type IniData:void { ? }

interface IniUtilsInterface {
RequestResponse:
	parseIniFile(string)(IniData)
}

outputPort IniUtils {
Interfaces: IniUtilsInterface
}

embedded {
Java:
	"joliex.util.IniUtils" in IniUtils
}
