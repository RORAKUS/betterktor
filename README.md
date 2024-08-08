# Better Ktor

A really simple library allowing **Folder routing** for Ktor.

## Contents

1. [Contents](#contents)
2. [Quickstart](#quickstart)
3. [Configuration](#configuration)
4. [Routes](#routes)
5. [Websockets](#web-sockets)
6. [Error handlers](#error-handlers)
7. [Named method routes](#named-method-routes)
8. [Custom path](#custom-path)
    * [Regex path](#regex-path)
9. [Name transforming](#name-transforming)
10. [Other info](#other-info)

## Quickstart

Better Ktor (BK) is available on Maven central.
The minimum version supported is **Java 11**.

### Maven

```xml

<dependency>
    <groupId>codes.rorak</groupId>
    <artifactId>betterktor</artifactId>
    <version>1.1.1</version>
</dependency>
```

### Gradle

```kotlin
implementation("codes.rorak:betterktor:1.1.1");
```

To quickly start with BK, just install the plugin in your main Ktor module:

```kotlin
import codes.rorak.betterktor.BKPlugin;

fun Application.module() {
	install(BKPlugin);
}
```

Now just create a package called `endpoints` and place this code into a file called `Test.kt`:

```kotlin
class Test: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("Hello, world!");
	}
}
```

If you now run your app and go to `ip:port/test` a message _"Hello world"_ is shown.

## Configuration

```kotlin
install(BKPlugin) {
	endpointsPackage = "endpoints";
	basePackage = "com.example";
	casing = BKTransform::kebabCase;
	rootPath = "/api";
	installWebSockets = true;
};
```

* **`endpointsPackage`** - name of the package, where endpoints are stored
* **`basePackage`** - name of the package, where your files are - if null, BK will try
  to [figure out](#the-base-package-insight)
* **`casing`** - a method transforming the name of your file & package into the endpoint name.
  See [casing](#name-transforming)
* **`rootPath`** - base path for HTTP routes for all endpoints
* **`installWebSockets`** - whether to install websockets inside BKPlugin if they are needed. Set this to _false_ if you
  want to configure them yourself

## Routes

Using the interface `Route` you can easily create a route. The path of the route will be:

```
config.rootPath + package inside endpoints + name of the class
```

So a path for `com.example.endpoints.user.SomeClass` with `config.rootPath = "/api"` would be
`/api/user/some-class`.

The interface provides all HTTP methods to override twice. You can choose to use the simple implementation
with just one parameter `call`, or if you use `call.request` a lot, you can also use an implementation with
two parameters - `call` and `request`!

Here is an example implementation:

```kotlin
class User: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText(request.cookies["cookie"]);
	}
	override suspend fun post(call: ApplicationCall) {
		call.respond(UserObject("Peter", 11));
	}
}
```

## Web sockets

BK also supports web socket routes. Using the interface `BKWebsocket` you can easily create and manage a socket!
Paths work the same as with [Routes](#routes), but the methods the interfaces provides are much simpler.
There is just one method in three overloads - `handle`. This method requires a websocket session as it's first
parameter,
but if you want to also have the `call` and the `request` parameter - also possible:

`suspend fun handle(session: DefaultWebSocketServerSession, call: ApplicationCall, request: ApplicationRequest) {};` <br>
`suspend fun handle(session: DefaultWebSocketServerSession) {};` <br>
`suspend fun handle(session: DefaultWebSocketServerSession, call: ApplicationCall) {};`

Here is an example:

```kotlin
class User: BKWebsocket {
	override suspend fun handle(session: DefaultWebSocketServerSession) {
		session.send("Hello there!");
		session.close(CloseReason(CloseReason.Codes.NORMAL), "Bye!");
	}
}
```

## Error handlers

And the last interface is `BKErrorHandler` and it's the simplest one. You have just one method:
`onError(call, cause)`. Paths work the same as with [Routes](#routes), but the class name **is not added to the path!**.

Here is an example:

```kotlin
class UserErrorHandler: BKErrorHandler {
	override suspend fun onError(call: ApplicationCall, cause: Throwable) {
		call.respondText(cause.message ?: "Error!");
	}
}
```

## Named method routes

It might be a little annoying to create a new class for every route,
so you have an option to create more routes in the class using normal methods.
Start by creating a route like you would normally, maybe try also adding a normal GET method.

```kotlin
class User: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("Hello user!");
	}
}
```

Now create a new method and name it how you want the route be named.
Don't forget that the method name will be also transformed by `config.casing`.

```kotlin
class User: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("Hello user!");
	}
	override suspend fun findUser(call: ApplicationCall) {
		call.respond(UserName("user_123", "User 123"));
	}
}
```

For these named routes you can also use two parameters, as with normal method routes.

But if you try going to `ip:port/user/find-user`, nothing is happening, why?
It's because all named routes are `POST` by default, but this behaviour can be changed using:

* The `@BKGet` annotation for the method. Easy and simple! (`@BKPost` also exists)
* For other methods, such as PUT or DELETE, you can use the `@BKMethod(method)` annotation and choose a method there
* To override this default behaviour for the class, use `@BKDefaultMethod(method)` (for the class)

Here is an example:

```kotlin
@BKDefaultMethod(BKHttpMethod.DELETE)
class User: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("Hello user!");
	}
	
	@BKGet
	override suspend fun findUser(call: ApplicationCall) {
		call.respondText("GET /user/find-user");
	}
	
	@BKPost
	override suspend fun editUser(call: ApplicationCall) {
		call.respondText("POST /user/edit-user");
	}
	
	@BKMethod(BKHttpMethod.PUT)
	override suspend fun newUser(call: ApplicationCall) {
		call.respondText("PUT /user/new-user");
	}
	override suspend fun deleteUser(call: ApplicationCall) {
		call.respondText("DELETE /user/delete-user");
	}
}
```

Private methods will be ignored, but if you still want to ignore a method, use a `@BKIgnore` annotation.
You can use this with [Routes](#routes), [Websockets](#web-sockets) or [Error handlers](#error-handlers).

## Custom path

Using an annotation `@BKPath(path)`, you can set a custom path for your endpoint.
You can either set an **absolute path**, or a **relative path**.

An absolute path must start with `/` and is, surprisingly, absolute. Not even `config.rootPath` will be added.
A relative path will on the other hand replace just the class name in the path, so even the package stays in the path.
**You can use any pattern features, like with routing** (tailcard, wildcard...).

Here is an example:

```kotlin
@BKPath("user-{id}") // relative path
class User: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respond(call.parameters["id"]);
	}
}
```

### Regex path

Regex path works exactly the same as a [Custom path](#custom-path), but you can use regex.
The annotation is `@BKRegexPath` (who would have guessed) with one parameter `path`.
You don't need an example...

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