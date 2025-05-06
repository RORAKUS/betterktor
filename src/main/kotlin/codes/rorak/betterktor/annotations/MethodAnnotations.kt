package codes.rorak.betterktor.annotations

/**
 * For a function or method endpoint this annotation sets the current HTTP method to use.
 * For a class endpoint this annotation sets the default HTTP method for all named routes.
 *
 * @param method The HTTP method to use
 * @param custom If you want to use a custom method, this string parameter allows you to.
 *  If you set this, don't set the [method] parameter
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Method(val method: HttpMethodOption = HttpMethodOption.GET, val custom: String = "");

/**
 * For method endpoint sets the current function to act as a selected HTTP method for that current endpoint.
 * For a function endpoint it does basically the same thing - the function will act as a selected HTTP
 * method for the endpoint determined by the package name (or otherwise if the [Path] annotation is set).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointMethod(val method: HttpMethodOption = HttpMethodOption.GET, val custom: String = "");

/**
 * A shortcut for `Method(HttpMethodOption.ANY)`
 * @see Method
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AnyCall;

/**
 * A shortcut for `Method(HttpMethodOption.GET)`
 * @see Method
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Get;

/**
 * A shortcut for `Method(HttpMethodOption.POST)`
 * @see Method
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Post;

/**
 * A shortcut for `Method(HttpMethodOption.PUT)`
 * @see Method
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Put;

/**
 * A shortcut for `Method(HttpMethodOption.DELETE)`
 * @see Method
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Delete;

/**
 * A shortcut for `Method(HttpMethodOption.PATCH)`
 * @see Method
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Patch;

/**
 * A shortcut for `EndpointMethod(HttpMethodOption.ANY)`
 * @see EndpointMethod
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointAnyCall;

/**
 * A shortcut for `EndpointMethod(HttpMethodOption.GET)`
 * @see EndpointMethod
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointGet;

/**
 * A shortcut for `EndpointMethod(HttpMethodOption.POST)`
 * @see EndpointMethod
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointPost;

/**
 * A shortcut for `EndpointMethod(HttpMethodOption.PUT)`
 * @see EndpointMethod
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointPut;

/**
 * A shortcut for `EndpointMethod(HttpMethodOption.DELETE)`
 * @see EndpointMethod
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointDelete;

/**
 * A shortcut for `EndpointMethod(HttpMethodOption.PATCH)`
 * @see EndpointMethod
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EndpointPatch;

/**
 * A handler method for the websocket endpoint class
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class WebsocketHandle;

/**
 * A handler method for the SSE endpoint class
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SSEHandle;

/**
 * A handler method for the Error handler endpoint class
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ErrorHandlerHandle;