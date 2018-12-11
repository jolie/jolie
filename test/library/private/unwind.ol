/*
t <- [ v : [ 1 {} , 2 {} , 3 {} ] , B {} ] 

|__A
|  |__1
|  |__2 
|  |__3
|   
|__B

unwind( t, B ) <- [  v : [ A : [ 1 {} ], B {} ], v : [ A : [ 2 {} ], B {} ], v : [ A : [ 3 {} ], B {} ] ]

|__v 
|  |__A  
|  |  |__1
|  |
|  |__B
|
|__v 
|  |__A  
|  |  |__2
|  |
|  |__B
|
|__v 
   |__A  
   |  |__3
   |
   |__B
*/

define unwind
{
  with( unwindRequest ){
    .data << bios;
    .query = "awards"
  }
}