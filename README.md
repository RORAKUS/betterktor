# Better Ktor

A really simple library allowing **Folder routing** for Ktor.

## Contents

1. [Contents](#contents)
2. [Quickstart](#quickstart)
3. [Configuration](#configuration)
4. [Route methods](#route-methods)
5. [Custom path](#custom-path)
    * [Regex](#regex)
6. [Name transforming](#name-transforming)
7. [Other info](#other-info)
    * [The base package insight](#the-base-package-insight)

## Quickstart

To use Better Ktor (BK from now) you just install the plugin:

```kotlin
fun Application.module() {
	install(BKPlugin);
}
```

Now just create a package `endpoints` and place this code inside a file called `Test.kt`:

```kotlin
class Test: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Hello, world!");
	}
}
```

Run the app and now when you open your page at `ip:port/test`, you will see a message - _Hello , world!_

## Configuration

You can, of course, configure the plugin to your needs. Here is how the configuration looks like:

```kotlin
install(BKPlugin) {
	packageName = "endpoints";
	basePackage = "com.example";
	casing = BKTransform::kebabCase;
	rootPath = "/api";
};
```

* **`packageName`** - the name of the package containing the endpoints
* **`basePackage`** - the name of the base package - if null, the program will try
  to [figure it out](#the-base-package-insight)
* **`casing`** - a `(String) -> String` method transforming the name of the file/package into the endpoint name. Use the
  class `BKTransform` for pre-set casings.
* **`rootPath`** - the base HTTP path for the endpoints (without the ending `/`)

## Route methods

The interface `BKRoute` supports all these HTTP methods: `GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS`.

This is how the method implementation should look: <br>
`override suspend fun methodName(call: ApplicationCall, request: ApplicationRequest) { /* code */ }`
The parameter `request` is just `call.request` for easier access.

## Custom path

Using the annotation `BKPath` you can set a custom path for the route.
If the path starts with `/`, it is completely absolute, ignoring even the `rootPath`.
If it doesn't, it is relative to it's package & root path. You can also use `Routing` path patterns.
Example:

```kotlin
@BKPath("/apis/users/user/{username}")
class User: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Hello, ${call.parameters["username"]}");
	}
}
```

When you enter `ip:port/apis/users/user/test`, you should get a response _Hello, test_, no matter where you put
the `User` class.

### Regex

The `BKPath` annotation also has a second parameter - `regex`.
This parameter will be combined with the path from the fist parameter (if it's provided) and work as a normal routing
regex.
Note that the entire path is going to be treated as a regex, if you specify this parameter, so do not forget to escape
regex characters.

```kotlin
@BKPath("/api/user/delete", regex = "(?<id>\\d+)") // The '/' is inserted automatically
class DeleteUser: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("User with id ${call.parameters["id"]} was deleted!");
	}
}
```

Now when you `GET ip:port/api/user/delete/86`, you should get _User with id 86 was deleted!_ as a response.

## Name transforming

For transforming names BK provides an object - `BKTransform`.
Inside you have 5 main casings methods - `camelCase`, `snake_case`, `kebab-case`, `PascalCase` and `Train-Case`,
as well as a method `String.toInternCase()`, which converts any casing into _`intern§casing`_.
All the other methods use this intern casing to transform any name into specified casing.
For example, here is the implementation of `camelCase`:

```kotlin
fun camelCase(s: String): String = "§([a-z])".toRegex().replace(s.toInternCase()) { it.groupValues[1].uppercase() };
```

You can expand this object using extension methods and the `toInternCase` method.

## Other info

### The base package insight

This is how the program figures out the base package of the class, so if anything goes wrong, try setting
the `basePackage` property:

```
for every $entry in the call stack:
  if $entry.fullName starts with any of these:
    $current-package, java.lang, io.ktor
  skip it
  
  else: return $entry.package
```