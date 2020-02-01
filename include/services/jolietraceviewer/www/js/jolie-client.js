var JolieClient = JolieClient || (function() {
    var API = {};
    var isError = function( data ) {
        if ( data != null && typeof data.error != "undefined" ) {
            return true;
        }
        return false;
    }
    
    var jolieCall = function( operation, request, callback, errorHandler ) {        

        $.ajax({
            url: '/' + operation,
            dataType: 'json',
            data: JSON.stringify( request ),
            type: 'POST',
            contentType: 'application/json;charset=UTF-8',
            success: function( data ){                
                if ( isError( data ) ) {
                    errorHandler( data );
                } else {
                    callback( data );
                }
            },
            error: function(errorType, textStatus, errorThrown) {                
                errorHandler( textStatus );
            }
        });
    }
    

    API.getTrace = function( request, callback, errorHandler ) {
        jolieCall( "getTrace", request, callback, errorHandler );
    }
    
    API.getTraceLine = function( request, callback, errorHandler ) {
        jolieCall( "getTraceLine", request, callback, errorHandler );
    }

    API.getTraceList = function( request, callback, errorHandler ) {
        jolieCall( "getTraceList", request, callback, errorHandler );
    }
    
    API.getServiceFile = function( request, callback, errorHandler ) {
        jolieCall( "getServiceFile", request, callback, errorHandler );
    }   
    
    return API;
})();
