include "JesterUtilsInterface.iol"
include "string_utils.iol"
include "runtime.iol"

execution{ concurrent }

outputPort MySelf {
    Interfaces: JesterUtilsInterface
}

inputPort JesterUtils {
    Location: "local"
    Interfaces: JesterUtilsInterface
}

init {
    getLocalLocation@Runtime()( MySelf.location )
}

main {

    [ getParamList( request )( response ) {
        response = false;
        splr = request
        splr.regex = "\\?"
        split@StringUtils( splr )( splres );
        pathpart = splres.result[ 0 ]
        querypart = splres.result[ 1 ]

        // path part
        splr = pathpart;
        splr.regex =  "/";
        split@StringUtils( splr )( splres );
        for( pr = 0, pr < #splres.result, pr++ ) {
            w = splres.result[ pr ];
            w.regex = "\\{(.*)\\}";
            find@StringUtils( w )( params );
            if ( params == 1 ) {
                response = true;
                response.path[ #response.path ] = params.group[1]
            }
        }

        // query part
        if ( !( querypart instanceof void ) )  {
            splr = querypart;
            splr.regex =  "&|=";
            split@StringUtils( splr )( splres );
            for( pr = 0, pr < #splres.result, pr++ ) {
                w = splres.result[ pr ];
                w.regex = "\\{(.*)\\}";
                find@StringUtils( w )( params );
                if ( params == 1 ) {
                    response = true;
                    response.query[ #response.query ] = params.group[1]
                }
            }
        }
    }]

    /* private operations */
    [ checkBranchChoiceConsistency( request )( response ) {
        response = true
        if ( request.branch instanceof TypeLink ) {
            check_rq = request.branch.link_name
            check_rq.type_map -> request.type_map
            checkTypeConsistency@MySelf( check_rq )( response )
        } else if ( request.branch instanceof TypeChoice ) {
             left_branch_rq.branch -> request.branch.choice.left_type
             left_branch_rq.type_map -> request.type_map
             checkBranchChoiceConsistency@MySelf(  left_branch_rq )( response )
             right_branch_rq.branch -> request.branch.choice.right_type
             right_branch_rq.type_map -> request.type_map
             checkBranchChoiceConsistency@MySelf( right_branch_rq )( response )
        } else if ( request.branch instanceof TypeInLine ) {
            if ( !is_defined( request.branch.root_type.void_type ) ) {
                throw( DefinitionError, "" )
            } 
        }
    }]


    [ checkTypeConsistency( request )( response ) {

        current_type -> request.type_map.( request );
        scope( analysis ) {
            install( DefinitionError => {
                error_msg = "Type " + current_type.name + ": root native type must be void"
                throw( DefinitionError, error_msg )
            })
            // link
            if ( current_type.type instanceof TypeLink ) {
                check_rq = current_type.type.link_name
                check_rq.type_map -> request.type_map
                checkTypeConsistency@MySelf( check_rq )( response )
            } 
            
            // choice
            else if ( current_type.type instanceof TypeChoice ) 
            {
                left_branch_rq.branch -> current_type.type.choice.left_type
                left_branch_rq.type_map -> request.type_map
                checkBranchChoiceConsistency@MySelf( left_branch_rq )( response )
                right_branch_rq.branch -> current_type.type.choice.right_type
                right_branch_rq.type_map -> request.type_map
                checkBranchChoiceConsistency@MySelf( right_branch_rq )( response )
            }
            // usual typeinline
            else if ( !is_defined( current_type.type.root_type.void_type ) ) {
                throw( DefinitionError, "" )
            }

            
            else {
                response = true
            }
        }
    }]

    [ getActualCurrentType( request )( response ) {
        current_type -> request.type_map.( request )
         if ( current_type.type instanceof TypeLink ) {
            get_actual_ctype_rq = current_type.type.link_name
            get_actual_ctype_rq.type_map -> request.type_map
            getActualCurrentType@MySelf( get_actual_ctype_rq )( response )
        } else {
            response = current_type.name
        } 
    }]
}