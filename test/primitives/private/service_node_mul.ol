

service MulService(factor: int){
    inputPort ip { 
        location: "local"
        requestResponse: multiply(int)(int)
    }

    main {
        multiply(req)(res){
            res = req * factor
        }
    }
}