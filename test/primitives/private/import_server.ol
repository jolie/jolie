from .imports.iface import fooIface

service main {
    inputPort importServer {
        Interfaces: fooIface
        Location: "local"
    }

    execution{ concurrent }

    init {
        install( err => nullProcess )
    }

    main{
        [fooOp(req)(res){
            if (req.a == "err"){
                throw( err, {msg = "error"} )
            } else {
                res.b = "success"
            }
        }]

        [barOp(req)]{
            nullProcess
        }
    }
}
