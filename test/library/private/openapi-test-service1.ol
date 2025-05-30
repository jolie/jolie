type Order: void {
	petId?:long
	quantity?:int
	id?:long
	shipDate?:string
	complete?:bool
	status?:string( enum( ["placed", "approved", "delivered"] ) )
}
type Category: void {
	name?:string
	id?:long
}
type User: void {
	firstName?:string
	lastName?:string
	password?:string
	userStatus?:int
	phone?:string
	id?:long
	email?:string
	username?:string
}
type Tag: void {
	name?:string
	id?:long
}
type Pet: void {
	photoUrls[0,*]: string
	name:string // Example: doggie
	id?:long
	category?:Category
	tags[0,*]: Tag
	status?:string( enum( ["available", "pending", "sold"] ) )
}
type ApiResponse: void {
	code?:int
	type?:string
	message?:string
}
type addPetRequest: void {
	body: Pet
}
type addPetResponse: undefined
type updatePetRequest: void {
	body: Pet
}
type updatePetResponse: undefined
type getUserByNameRequest: void {
	pusername:string
}
type getUserByNameResponse: User
type deleteUserRequest: void {
	pusername:string
}
type deleteUserResponse: undefined
type updateUserRequest: void {
	pusername:string
	body: User
}
type updateUserResponse: undefined
type findPetsByStatusRequest: void {
	qstatus[0,*]: string( enum( ["available", "pending", "sold"] ) )
}
type findPetsByStatusResponse: void {
	pets[0,*]:Pet
}
type createUsersWithListInputRequest: void {
	body[0,*]: User
}
type createUsersWithListInputResponse: undefined
type uploadFileRequest: void {
	ppetId:long
	additionalMetadata?:string
	file?:raw
}
type uploadFileResponse: ApiResponse
type getInventoryRequest: void {
}
type getInventoryResponse: undefined
type loginUserRequest: void {
	qusername:string
	qpassword:string
}
type loginUserResponse: string
type createUserRequest: void {
	body: User
}
type createUserResponse: undefined
type createUsersWithArrayInputRequest: void {
	body[0,*]: User
}
type createUsersWithArrayInputResponse: undefined
type findPetsByTagsRequest: void {
	qtags[0,*]: string
}
type findPetsByTagsResponse: void {
	pets[0,*]:Pet
}
type placeOrderRequest: void {
	body: Order
}
type placeOrderResponse: Order
type logoutUserRequest: void {
}
type logoutUserResponse: undefined
type updatePetWithFormRequest: void {
	ppetId:long
	name?:string
	status?:string
}
type updatePetWithFormResponse: undefined
type getPetByIdRequest: void {
	ppetId:long
}
type getPetByIdResponse: Pet
type deletePetRequest: void {
	apikey?:string
	ppetId:long
}
type deletePetResponse: undefined
type getOrderByIdRequest: void {
	porderId:long( ranges( [1L, 0L] ) )
}
type getOrderByIdResponse: Order
type deleteOrderRequest: void {
	porderId:long( ranges( [1L, *] ) )
}
type deleteOrderResponse: undefined

interface testInterface {
RequestResponse:
	addPet( addPetRequest )( addPetResponse ) throws Fault400( string )  Fault422( string )  Fault500( string ) ,
	updatePet( updatePetRequest )( updatePetResponse ) throws Fault400( string )  Fault422( string )  Fault500( string )  Fault404( string ) ,
	getUserByName( getUserByNameRequest )( getUserByNameResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	deleteUser( deleteUserRequest )( deleteUserResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	updateUser( updateUserRequest )( updateUserResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	findPetsByStatus( findPetsByStatusRequest )( findPetsByStatusResponse ) throws Fault400( string )  Fault500( string ) ,
	createUsersWithListInput( createUsersWithListInputRequest )( createUsersWithListInputResponse ) throws Faultdefault( string )  Fault500( string ) ,
	uploadFile( uploadFileRequest )( uploadFileResponse ) throws Fault500( string ) ,
	getInventory( getInventoryRequest )( getInventoryResponse ) throws Fault500( string ) ,
	loginUser( loginUserRequest )( loginUserResponse ) throws Fault400( string )  Fault500( string ) ,
	createUser( createUserRequest )( createUserResponse ) throws Faultdefault( string )  Fault500( string ) ,
	createUsersWithArrayInput( createUsersWithArrayInputRequest )( createUsersWithArrayInputResponse ) throws Faultdefault( string )  Fault500( string ) ,
	findPetsByTags( findPetsByTagsRequest )( findPetsByTagsResponse ) throws Fault400( string )  Fault500( string ) ,
	placeOrder( placeOrderRequest )( placeOrderResponse ) throws Fault400( string )  Fault500( string ) ,
	logoutUser( logoutUserRequest )( logoutUserResponse ) throws Faultdefault( string )  Fault500( string ) ,
	updatePetWithForm( updatePetWithFormRequest )( updatePetWithFormResponse ) throws Fault400( string )  Fault422( string )  Fault500( string ) ,
	getPetById( getPetByIdRequest )( getPetByIdResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	deletePet( deletePetRequest )( deletePetResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	getOrderById( getOrderByIdRequest )( getOrderByIdResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) ,
	deleteOrder( deleteOrderRequest )( deleteOrderResponse ) throws Fault400( string )  Fault500( string )  Fault404( string ) 
}

service test {
  inputPort DEMO {
    location: "local"
    protocol: sodep
    interfaces: testInterface
  }

  main {
    nullProcess
  }
}
