
interface BIface {
    RequestResponse: version(void)(string)
}


service B {

    inputPort ip1 {
        Location: "local"
        Interfaces: BIface
    }

    main{
        [version(req)(res){
            res = "1.1.0"
        }]
    }
}