package codes.rorak.betterktor.annotations

import codes.rorak.betterktor.BetterKtorConfig
import codes.rorak.betterktor.api.BetterKtor
import io.ktor.server.application.*

/**
 * The class, method, function or parameter annotated with this annotation will be ignored by BetterKtor
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Ignore;

/**
 * The return value of the method or function annotated with this annotation will be ignored.
 * It will be registered as if the return value was [Unit]
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class IgnoreReturn;

/**
 * Sets the path for the class or function endpoint
 *
 * @param path The string path for the endpoint
 * @param regex If the path should be a regex path. Default: `false`
 * @param relativeTo Relative option. Defaultly set to [RelativeOption.DEFAULT]
 * @see RelativeOption
 * @see RelativeOption.DEFAULT
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Path(val path: String, val regex: Boolean, val relativeTo: RelativeOption = RelativeOption.DEFAULT);

/**
 * Sets the relativeness for an endpoint. Defaultly all endpoints are relative to its parent.
 * If an endpoint has this annotation together with the [Path] annotation with `relativeTo` option set,
 * the value of this annotation will be used.
 *
 * @param option The option to use
 * @see Path
 * @see RelativeOption
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RelativeTo(val option: RelativeOption);

/**
 * Sets the current endpoint as a normal one. Useful for non-normal endpoint classes annotated
 * with [Websocket], [SSE] or [ErrorHandler].
 *
 * Can be also used to override the default naming behaviour (`get()` method annotated with
 * `@Endpoint` will not be treated as a GET method but a `/get` endpoint)
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Endpoint;

/**
 * Sets the current endpoint as a websocket one. If used with a class, all its methods will be treated
 * as websockets.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Websocket;

/**
 * Sets the current endpoint as an SSE (Server-Sent Event) endpoint. If used with a class, all its methods
 * will be treated as SSE endpoints.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SSE;

/**
 * Sets the current endpoint as an error handler. If used with a class, all its methods will be
 * treated as error handlers.
 *
 * @param type The error handler type - if it is for normal request, websockets or SSEs
 * When not specified, the error resolver will firstly try to find the type using
 * parameter types and if unsuccessful it chooses [ErrorHandlerOption.NORMAL]
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ErrorHandler(val type: ErrorHandlerOption = ErrorHandlerOption.DEFAULT);

/**
 * Injects the selected value to the annotated property, so the property contains the actual value
 * from the call on run. **Beware:** while using `object` endpoints without [Mutex],
 * the values of the properties will change during runtime with every call.
 *
 * The properties annotated with this annotation must have the correct type specified for each [InjectOption].
 * If the type is not nullable, it will throw an exception when the field is either not accessible
 * (for example sessions in normal calls) or the value is not found (for example a concrete parameter).
 *
 * @param option The field to inject
 * @param parameter The parameter for the injection. Every option specifies, if it needs it or not.
 *
 * @see InjectOption
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Inject(val option: InjectOption, val parameter: String = "");

/**
 * Injects the [ApplicationCall] into the property. **Beware:** while using `object` endpoints with `suspend` methods,
 * the values of the properties will change during runtime with every call.
 *
 * The properties annotated with this annotation must have the [ApplicationCall] type.
 *
 * @see Inject
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InjectCall;

/**
 * Sets the authentication for the endpoint.
 * If a class is annotated with this, it will set the authentication for all its endpoints.
 *
 * @param id The id of the authentication to use. Default value is taken from [BetterKtorConfig.defaultAuthId]
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Auth(val id: String = "", val option: AuthOption = AuthOption.REQUIRED);

/**
 * Sets the casing option for the annotated endpoint and all its children.
 *
 * @param value The option for the casing
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Casing(val value: CasingOption);

/**
 * Makes the endpoint annoted synchronous - request will run continuosly
 * without a possiblity for an external variable change.
 *
 * @param name Specifies the name for the mutex. All endpoints with the same mutex name
 * will wait on each other. If not specified, a default mutex will be used.
 *
 * @see BetterKtor.mutexMap
 * @see BetterKtor.defaultMutex
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Mutex(val name: String = "default");

/**
 * For methods inside a class annotated with [Mutex] or when the
 * [BetterKtorConfig.useMutex] is set, this annotation removes
 * the mutex from the current method.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NoMutex;

/**
 * Marks the endpoint class as a complex websocket class
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ComplexWebsocket;

/**
 * Makes the annotated method a connection start handler for
 * complex websockets
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CWOnConnect;

/**
 * Makes the annotated method a connection close handler for
 * complex websockets
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CWOnClose;

/**
 * Makes the annotated method a message received handler for
 * complex websockets
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CWOnMessage;

/**
 * Makes the annotated method an error handler for
 * complex websockets
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CWOnError;

/**
 * Makes the annotated method a before message send handler for
 * complex websockets
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CWOnMessageSend;

/**
 * Sets the annotated property to be the flow for the complex websocket.
 * If it lays inside a companion object, it will be for every instance, if inside
 * a class, it will be just for the current one.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CWFlow;