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

    embed ChildService(p.child) as Child

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