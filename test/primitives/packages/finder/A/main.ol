from B import B

interface AIface {
    RequestResponse: version(void)(string)
}


service A {

    inputPort ip2 {
        Location: "local"
        Interfaces: AIface
    }

    embed B as B

    main{
        [version(req)(res){
            res = version@B()
        }]
    }
}