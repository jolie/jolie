type Order: void {
	.petId?:long
	.quantity?:int
	.id?:long
	.shipDate?:string
	.complete?:bool
	.status?:string
}
type User: void {
	.firstName?:string
	.lastName?:string
	.password?:string
	.userStatus?:int
	.phone?:string
	.id?:long
	.email?:string
	.username?:string
}
type Category: void {
	.name?:string
	.id?:long
}
type UserTest: void
type Tag: void {
	.name?:string
	.id?:long
}
type Pet: void {
	.photoUrls[0,*]: string
	.name:string
	.id?:long
	.category?:Category
	.tags[0,*]: Tag
	.status?:string
}
type ApiResponse: void {
	.code?:int
	.type?:string
	.message?:string
}
type addPetRequest: void {
	.body: Pet
}
type addPetResponse:undefined 
type updatePetRequest: void {
	.body: Pet
}
type updatePetResponse:undefined 
type getUserByNameRequest: void {
	._pusername:string
}
type getUserByNameResponse:User
type deleteUserRequest: void {
	._pusername:string
}
type deleteUserResponse:undefined 
type updateUserRequest: void {
	._pusername:string
	.body: User
}
type updateUserResponse:undefined 
type findPetsByStatusRequest: void {
	._qstatus[0,*]: string
}
type findPetsByStatusResponse: void {
	._[0,*]:Pet
}
type createUsersWithListInputRequest: void {
	.body[0,*]: User
}
type createUsersWithListInputResponse:undefined 
type uploadFileRequest: void {
	._ppetId:long
	.additionalMetadata?:string
	.file?:raw
}
type uploadFileResponse:ApiResponse
type createTestRequest: void {
	.body[0,*]: UserTest
}
type createTestResponse:undefined 
type getInventoryRequest: void {
}
type getInventoryResponse:undefined
type loginUserRequest: void {
	._qusername:string
	._qpassword:string
}
type loginUserResponse:string
type createUserRequest: void {
	.body: User
}
type createUserResponse:undefined 
type createUsersWithArrayInputRequest: void {
	.body[0,*]: User
}
type createUsersWithArrayInputResponse:undefined 
type findPetsByTagsRequest: void {
	._qtags[0,*]: string
}
type findPetsByTagsResponse: void {
	._[0,*]:Pet
}
type placeOrderRequest: void {
	.body: Order
}
type placeOrderResponse:Order
type logoutUserRequest: void {
}
type logoutUserResponse:undefined 
type updatePetWithFormRequest: void {
	._ppetId:long
	.name?:string
	.status?:string
}
type updatePetWithFormResponse:undefined 
type getPetByIdRequest: void {
	._ppetId:long
}
type getPetByIdResponse:Pet
type deletePetRequest: void {
	.api_key?:string
	._ppetId:long
}
type deletePetResponse:undefined 
type getOrderByIdRequest: void {
	._porderId:long
}
type getOrderByIdResponse:Order
type deleteOrderRequest: void {
	._porderId:long
}
type deleteOrderResponse:undefined 
interface SwaggerDemoOkInterface{
RequestResponse:
	addPet( addPetRequest )( addPetResponse ) throws Fault500( string )  Fault405( string ) ,
	updatePet( updatePetRequest )( updatePetResponse ) throws Fault400(Pet)  Fault500( string )  Fault404( string )  Fault405( string ) ,
	getUserByName( getUserByNameRequest )( getUserByNameResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	deleteUser( deleteUserRequest )( deleteUserResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	updateUser( updateUserRequest )( updateUserResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	findPetsByStatus( findPetsByStatusRequest )( findPetsByStatusResponse ) throws Fault400( string )  Fault500( string ) ,
	createUsersWithListInput( createUsersWithListInputRequest )( createUsersWithListInputResponse ) throws Fault500( string ) ,
	uploadFile( uploadFileRequest )( uploadFileResponse ) throws Fault500( string ) ,
	createTest( createTestRequest )( createTestResponse ) throws Fault500( string ) ,
	getInventory( getInventoryRequest )( getInventoryResponse ) throws Fault500( string ) ,
	loginUser( loginUserRequest )( loginUserResponse ) throws Fault400( string )  Fault500( string ) ,
	createUser( createUserRequest )( createUserResponse ) throws Fault500( string ) ,
	createUsersWithArrayInput( createUsersWithArrayInputRequest )( createUsersWithArrayInputResponse ) throws Fault500( string ) ,
	findPetsByTags( findPetsByTagsRequest )( findPetsByTagsResponse ) throws Fault400( string )  Fault500( string ) ,
	placeOrder( placeOrderRequest )( placeOrderResponse ) throws Fault400( string )  Fault500( string ) ,
	logoutUser( logoutUserRequest )( logoutUserResponse ) throws Fault500( string ) ,
	updatePetWithForm( updatePetWithFormRequest )( updatePetWithFormResponse ) throws Fault500( string )  Fault405( string ) ,
	getPetById( getPetByIdRequest )( getPetByIdResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	deletePet( deletePetRequest )( deletePetResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	getOrderById( getOrderByIdRequest )( getOrderByIdResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	deleteOrder( deleteOrderRequest )( deleteOrderResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) 
}


inputPort Test {
	Location:"local"
	Protocol: sodep
	Interfaces: SwaggerDemoOkInterface, SwaggerDemoInterface
}

main {nullProcess}
