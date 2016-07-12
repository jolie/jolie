
include "../AbstractTestUnit.iol"

define doTest
{
    animals.pets[0].name[0] = "cat";
    animals.pets[0].name[1] = "dog";
    animals.pets[0].name[2] = "parrot";

    numbers[0] = 7;
    numbers[1] = 3;
    numbers[2] = 10;

    i = 0;
    for_each( k -> animals.pets[0].name){
        if( k != animals.pets[0].name[i++] ){
            throw( TestFailed, "for_each doesn't work with subnodes " )
        }
    };
    if(i != 3 ){
        throw( TestFailed, "for_each doesn't work with subnodes " )
    };

    i = 0;
    for_each( k -> numbers){
        if( k != numbers[i++] ){
             throw( TestFailed, "for_each doesn't work with root nodes" )
        }
    };
    if(i != 3 ){
        throw( TestFailed, "for_each doesn't work with root nodes" )
    };

    for_each( k -> emptyArray){
        throw( TestFailed, "for_each doesn't work with empty nodes " )
    };

    i = 0;
    for_each( k[33] -> animals.pets[0].name){
    if( k[33] != animals.pets[0].name[i++] ){
       throw( TestFailed, "for_each doesn't work with the index " )
    }
    };
    if(i != 3 ){
       throw( TestFailed, "for_each doesn't work with the index " )
    };

    i = 0;
    for_each( k[33] -> numbers){
    if( k[33] != numbers[i++] ){
        throw( TestFailed, "for_each doesn't work with the index " )
    }
    };
    if(i != 3 ){
        throw( TestFailed, "for_each doesn't work with the index " )
    };

    for_each( k[33] -> emptyArray){
        throw( TestFailed, "for_each doesn't work with empty nodes " )
    }
}