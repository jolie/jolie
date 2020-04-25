from interface import twiceIface

inputPort IP{
    interfaces : twiceIface
    location: "socket://localhost:3000"
    protocol: sodep
}


main {
    [twice(req)(res){
        res = req * 2
    }]
}