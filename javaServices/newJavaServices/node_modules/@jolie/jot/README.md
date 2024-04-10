# jot - Jolie testing suite

![jot logo](./jot.png)

`jot` is a command-line tool for testing Microservice Architectures(MSAs).
The tool is developed using [Jolie](https://www.jolie-lang.org/), a service oriented programming language that empathize the structured linguistic approach to the programming of services.

`jot` provides a Jolie developer ability to develop a service that will act as a tester service in the project.
With the concept of tester service, it can also be used to test any services with the protocol that Jolie supports.

## Installing

Using npm:

```bash
# install in a project
npm install @jolie/jot
```

<!-- Using jpm (this method allows user to run the command directly from cli) 

```bash
jpm install jot
```
 -->

## Usage


## Running jot

Provide a field in package.json script to invoke jot through `npm run`

```bash
  "scripts": {
    "test": "jot [jot_configuration.json]"
  },
```

then you can invoke jot using `npm run test`

```bash
npm run test
```

## `jot` Configuration file

`jot` requires the user to provide detail for testing service in `json` format.

```json
{
  "test": "test", // path to test directory. Default: "test"
  "params": { // parameter for jot, key of this object defines the Jolie service that jot will launch
    "service.ol": [ 
      {
        "name": "main", // name of the service
        "params": { } // parameter to pass to the `main` service that reside in TestJot.ol
      }
    ]
  },
  "reporters": { // [OPTIONAL] reporter configuration
    "path": "", // path to custom reporter module
    "service": "" // reporter service
  }
}
```

See [examples](examples) folder for basic usage in practice.

## Annotation

Currently, `jot` supports 5 annotations for describing the test cycle between test service, namely `@BeforeAll`, `@BeforeEach`, `@Test`, `@AfterEach`, `@AfterAll`.

The operation invocation inside testing service
The life cycle for each service operation invocation is following, from top to bottom.

```bash
@BeforeAll
    |
@BeforeEach
    |
  @Test
    |
@AfterEach
    |
@AfterAll
```

*Note that the execution order for each operation is non-deterministic.*

## Customize Reporter

`jot` provides an ability to customize the test runner by providing a set of operations that a user may use to change the result to show on console. The default reporter is shown in [spec.ol](reporters/spec.ol)

```jolie
type Service {
    title: string // test service name
}

type Test {
    title: string // test operation name
}

type TestFailed {
    title: string // test operation name
    error: any // error message
}


/**
	metrics data collected by the test runner
*/
type Stats {
	tests: int
	passes: int
	failures: int
	services: int
	durations: long
}

/**
    interface that jot will use to communicate with the reporter
 */
interface ReporterInterface {
	RequestResponse: 
        // fires when the test runner finished it's instantiation
        eventRunBegin(void)(void),

        // fires when the test operation pass
        eventTestPass(Test)(void),

        // fires when the test operation fail
        eventTestFail(TestFailed)(void),

        // fires when a testing service finished it's instantiation
        eventServiceBegin(Service)(void),

        // fires when a testing service completes all the testing operations
        eventServiceEnd(Stats)(void),

        // fires when the test runner complete all the testing services
        eventRunEnd(void)(void)
}
```
