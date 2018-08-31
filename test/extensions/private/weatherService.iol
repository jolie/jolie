type NOTATIONType:any

type GetWeather:void {
	.CountryName?:string
	.CityName?:string
}

type GetCitiesByCountry:void {
	.CountryName?:string
}

type GetWeatherResponse:void {
	.GetWeatherResult?:string
}

type GetCitiesByCountryResponse:void {
	.GetCitiesByCountryResult?:string
}

type GetWeatherHttpPostIn:void {
	.CountryName:string
	.CityName:string
}

type GetWeatherHttpGetIn:void {
	.CountryName:string
	.CityName:string
}

interface GlobalWeatherHttpPost {
RequestResponse:
	GetWeather(GetWeatherHttpPostIn)(string),
	GetCitiesByCountry(string)(string)
}

interface GlobalWeatherSoap {
RequestResponse:
	GetWeather(GetWeather)(GetWeatherResponse),
	GetCitiesByCountry(GetCitiesByCountry)(GetCitiesByCountryResponse)
}

interface GlobalWeatherHttpGet {
RequestResponse:
	GetWeather(GetWeatherHttpGetIn)(string),
	GetCitiesByCountry(string)(string)
}

outputPort GlobalWeatherHttpPost {
Location: "socket://www.webservicex.net:80/globalweather.asmx"
Protocol: http
Interfaces: GlobalWeatherHttpPost
}

outputPort GlobalWeatherSoap12 {
Location: "socket://localhost:80/"
Protocol: soap
Interfaces: GlobalWeatherSoap
}

outputPort GlobalWeatherSoap {
Location: "socket://www.webservicex.net:80/globalweather.asmx"
Protocol: soap {
	.wsdl = "http://www.webservicex.net/globalweather.asmx?WSDL";
	.wsdl.port = "GlobalWeatherSoap"
}
Interfaces: GlobalWeatherSoap
}

outputPort GlobalWeatherHttpGet {
Location: "socket://www.webservicex.net:80/globalweather.asmx"
Protocol: http
Interfaces: GlobalWeatherHttpGet
}


