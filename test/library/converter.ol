include "../AbstractTestUnit.iol"
include "converter.iol"
include "string_utils.iol"
include "file.iol"

define doTest
{
	// plain strings

	str = "Matthias Dieter Wallnöfer";
	length@StringUtils(str)(len);
	if (len != 25) {
		throw( TestFailed, "strings invalid length/size" )
	};
	stringToRaw@Converter(str { .charset="latin1" })(buf);
	getSize@File(buf)(size);
	if (size != 25) {
		throw( TestFailed, "strings invalid length/size" )
	};
	rawToString@Converter(buf { .charset="latin1" })(str2);
	length@StringUtils(str2)(len);
	if (len != 25) {
		throw( TestFailed, "strings invalid length/size" )
	};
	if (str2 != str) {
		throw( TestFailed, "strings do not match" )
	};

	stringToRaw@Converter(str)(buf);
	str2 = string(buf);
	length@StringUtils(str2)(len);
	if (len != 25) {
		throw( TestFailed, "strings invalid length/size" )
	};
	if (str2 != str) {
		throw( TestFailed, "strings do not match" )
	};

	// base64

	stringToRaw@Converter(str { .charset="latin1" })(buf);
	rawToBase64@Converter(buf)(base64);
	length@StringUtils(base64)(len);
	getSize@File(base64)(size);
	if (len != 36 || size != 36) {
		throw( TestFailed, "base64 invalid length/size" )
	};
	base64ToRaw@Converter(base64)(buf2);
	rawToString@Converter(buf2 { .charset="latin1" })(str2);
	length@StringUtils(str2)(len);
	if (len != 25) {
		throw( TestFailed, "base64 invalid length/size" )
	};
	if (str2 != str) {
		throw( TestFailed, "base64 strings do not match" )
	};

	// getSize

	getSize@File(void)(size);
	if (size != 0) {
		throw( TestFailed, "void invalid size" )
	};
	getSize@File(false)(size);
	if (size != 1) {
		throw( TestFailed, "boolean invalid size" )
	};
	getSize@File(0)(size);
	if (size != 4) {
		throw( TestFailed, "int invalid size" )
	};
	getSize@File(0l)(size);
	if (size != 8) {
		throw( TestFailed, "long invalid size" )
	};
	getSize@File(1.0)(size);
	if (size != 8) {
		throw( TestFailed, "double invalid size" )
	}
}
