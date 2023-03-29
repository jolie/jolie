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
    test import an embedded service node
*/
from .private.services.service_node_mul import MulService
from .private.services.service_node_param_string import testStringParamService
from .private.services.service_node_param_custom import testDefinedTypeParamService
from .private.services.service_node_param_optional import testTypeOptionalParamService
from .private.services.service_node_multi_services import ParentService


interface TestUnitInterface {
RequestResponse:
	test(void)(void) throws TestFailed(any)
}

interface MulServiceInterface{
    requestResponse:multiply(int)(int)
}

service Main {
    execution { single }

    inputPort TestUnitInput {
        Location: "local"
        Interfaces: TestUnitInterface
    }

    embed testStringParamService("test") as testStringParamService

    embed testDefinedTypeParamService( 1 {
        a="test"
        b << {
            a=1
        }
        c=2
        d=2.0
    } ) as testDefinedTypeParamService

    embed testTypeOptionalParamService as testTypeOptionalParamService

    embed ParentService( { 
        a = "str" 
        child << {
            a = "str2"
            b = 2
        }
    } ) as ParentServ

    // test embed imported service with predeclared outputport
    outputPort MulService{
        interfaces: MulServiceInterface
    }

    embed MulService(5) in MulService

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