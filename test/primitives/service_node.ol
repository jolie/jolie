/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
 
/**
    Test syntax
*/

service testStringParamService(var : string) {

    inputPort ip {
        location:"local"
        requestResponse: testParam(void)(void) throws TestFailed(any)
    }

    main{
        testParam()(){
            if (!is_string(var)){
                throw(TestFailed, "passing argument is invalid type, expected \"string\"")
            }
        }
    }
}


/**
    A service with custom type
*/

type testParam :int {
    a:string
    b:void{
        a:int
    }
    c:int
    d:double
}

service testDefinedTypeParamService(p : testParam) {

    inputPort ip {
        location:"local"
        requestResponse: testParam(void)(void) throws TestFailed(any)
    }

    main{
        testParam()(){
            if (!(p instanceof testParam)){
                throw(TestFailed, "passing argument is invalid type, expected \"testParam\"")
            }
        }
    }
}

/**
    A service with optional custom type
*/
type testParamOptional : testParam | undefined

service testTypeOptionalParamService(p : testParamOptional) {

    inputPort ip{
        location:"local"
        requestResponse: testParam(void)(void) throws TestFailed(any)
    }

    main{
        testParam()(){
            if (!(p instanceof testParamOptional)){
                throw(TestFailed, "passing argument is invalid type, expected \"testParamOptional\"")
            }
        }
    }
}

/**
    Variable path node as an argument
*/

type parenParam: void{
    a : string
    child :childParam 
}

type childParam : void{
    a : string
    b : int
}

service ParentService(p : parenParam) {

    inputPort ip {
        location:"local"
        requestResponse: testParam(void)(void) throws TestFailed(any)
    }

    embed ChildService(p.child) in new Child

    main{
        testParam()(){
            if (!(p instanceof parenParam)){
                throw(TestFailed, "passing argument is invalid type, expected \"parenParam\"")
            }
            testParam@Child()()
        }
    }
}

service ChildService(p : childParam) {

    inputPort ip {
        location:"local"
        requestResponse: testParam(void)(void) throws TestFailed(any)
    }

    main{
        testParam()(){
            if (!(p instanceof childParam)){
                throw(TestFailed, "passing argument is invalid type, expected \"childParam\"")
            }
        }
    }
}


/**
    test import an embedded service node
*/
from .private.service_node_mul import MulService


interface TestUnitInterface {
RequestResponse:
	test(void)(void) throws TestFailed(any)
}


service main{

    execution { single }

    inputPort TestUnitInput {
        Location: "local"
        Interfaces: TestUnitInterface
    }

    embed testStringParamService("test") in new testStringParamService

    embed testDefinedTypeParamService( 1 {
        a="test"
        b << {
            a=1
        }
        c=2
        d=2.0
    } ) in new testDefinedTypeParamService

    embed testTypeOptionalParamService in new testTypeOptionalParamService

    embed ParentService( { 
        a = "str" 
        child << {
            a = "str2"
            b = 2
        }
    } ) in new ParentServ
    embed MulService(5) in new MulService

    define doTest {
        testParam@testStringParamService()()
        testParam@testDefinedTypeParamService()()
        testParam@testTypeOptionalParamService()()
        testParam@ParentServ()()
        multiply@MulService(2)(res)
        if (res != 10){
            throw(TestFailed, "expected value return from mul service to be " + 10 + ", received " + res)
        }
    }

    main
    {
        test()() {
            doTest
        }
    }
}