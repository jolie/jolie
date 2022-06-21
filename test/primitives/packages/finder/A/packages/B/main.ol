interface BIface {
    RequestResponse: version(void)(string)
}


service B {

    inputPort ip2 {
        Location: "local"
        Interfaces: BIface
    }

    main{
        [version(req)(res){
            res = "1.0.0"
        }]
    }
}