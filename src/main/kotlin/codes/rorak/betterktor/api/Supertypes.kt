package codes.rorak.betterktor.api

import codes.rorak.betterktor.BetterKtorConfig
import codes.rorak.betterktor.annotations.Inject
import codes.rorak.betterktor.annotations.InjectCall
import codes.rorak.betterktor.annotations.InjectOption
import codes.rorak.betterktor.annotations.Mutex
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.server.application.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlin.reflect.KClass

interface BetterKtorEndpoint;

/**
 * Endpoints extending this abstract class will automatically have an [ApplicationCall]
 * injected.
 *
 * @see InjectCall
 */
abstract class CallInject {
	/**
	 * The injected call. During an endpoint handler execution, this
	 * property will contain the current call instance.
	 * **Beware**: When using `object` endpoints without the [Mutex] annotation,
	 * this will not work.
	 *
	 * @see InjectCall
	 */
	@InjectCall
	protected lateinit var call: ApplicationCall;
}

/**
 * A superclass for endpoints extending the [CallInject] class while
 * implementing the [Endpoint] interface
 */
abstract class ICEndpoint: CallInject(), Endpoint;

/**
 * A superclass for websocket endpoints extending the [CallInject] class while
 * implementing the [Websocket] interface
 */
abstract class ICWebsocket: CallInject(), Websocket;

/**
 * A superclass for SSE endpoints extending the [CallInject] class while
 * implementing the [SSE] interface
 */
abstract class ICSSE: CallInject(), SSE;

/**
 * A superclass for error handlers extending the [CallInject] class while
 * implementing the [ErrorHandler] interface
 */
abstract class ICErrorHandler<T: Throwable>: CallInject(), ErrorHandler<T>;

/**
 * The main interface to implement when you want a
 * type safe simple predefined HTTP methods in your
 * endpoints, or when the [BetterKtorConfig.strict] mode is set.
 */
interface Endpoint: BetterKtorEndpoint {
	/**
	 * A normal HTTP GET method without parameters. You can use an
	 * injected call with it.
	 */
	suspend fun get() {}
	
	/**
	 * A normal HTTP GET method
	 * @param call The [ApplicationCall] of the request
	 */
	suspend fun get(call: ApplicationCall) {}
	
	
	/**
	 * A normal HTTP POST method without parameters. You can use an
	 * injected call with it.
	 */
	suspend fun post() {}
	
	/**
	 * A normal HTTP POST method
	 * @param call The [ApplicationCall] of the request
	 */
	suspend fun post(call: ApplicationCall) {}
	
	
	/**
	 * A normal HTTP PUT method without parameters. You can use an
	 * injected call with it.
	 */
	suspend fun put() {}
	
	/**
	 * A normal HTTP PUT method
	 * @param call The [ApplicationCall] of the request
	 */
	suspend fun put(call: ApplicationCall) {}
	
	
	/**
	 * A normal HTTP DELETE method without parameters. You can use an
	 * injected call with it.
	 */
	suspend fun delete() {}
	
	/**
	 * A normal HTTP DELETE method
	 * @param call The [ApplicationCall] of the request
	 */
	suspend fun delete(call: ApplicationCall) {}
	
	
	/**
	 * A normal HTTP PATCH method without parameters. You can use an
	 * injected call with it.
	 */
	suspend fun patch() {}
	
	/**
	 * A normal HTTP PATCH method
	 * @param call The [ApplicationCall] of the request
	 */
	suspend fun patch(call: ApplicationCall) {}
	
	
	/**
	 * A method handling all other HTTP methods without parameters.
	 * The concrete method handlers will run first. You can use an
	 * injected call with it.
	 */
	suspend fun any() {}
	
	/**
	 * A method handling all other HTTP methods.
	 * The concrete method handlers will run first.
	 * @param call The [ApplicationCall] of the request
	 */
	suspend fun any(call: ApplicationCall) {}
}

/**
 * The main interface to implement when you want a
 * type safe simple predefined HTTP methods in your
 * websocket endpoints, or when the [BetterKtorConfig.strict] mode is set.
 */
interface Websocket: BetterKtorEndpoint {
	/**
	 * Handles a websocket connection without parameters.
	 * You can obtain the session or the call using injecting.
	 *
	 * @see Inject
	 */
	suspend fun websocket() {}
	
	/**
	 * Handles a websocket connection.
	 * @param session The websocket session
	 */
	suspend fun websocket(session: DefaultWebSocketServerSession) {}
	
	/**
	 * Handles a websocket connection.
	 * @param session The websocket session
	 * @param call A shortcut for the call obtained with `session.call`
	 */
	suspend fun websocket(session: DefaultWebSocketServerSession, call: ApplicationCall) {}
}

/**
 * The main interface to implement when you want a
 * type safe simple predefined HTTP methods in your
 * SSE endpoints, or when the [BetterKtorConfig.strict] mode is set.
 */
interface SSE: BetterKtorEndpoint {
	/**
	 * Handles an SSE connection without parameters.
	 * You can obtain the session or the call using injecting.
	 *
	 * @see Inject
	 */
	suspend fun sse() {}
	
	/**
	 * Handles an SSE connection.
	 * @param session The SSE session
	 */
	suspend fun sse(session: ServerSSESession) {}
	
	/**
	 * Handles an SSE connection.
	 * @param session The SSE session
	 * @param call A shortcut for the call obtained with `session.call`
	 */
	suspend fun sse(session: ServerSSESession, call: ApplicationCall) {}
}

/**
 * The main interface to implement when you want a
 * type safe simple predefined error handler.
 *
 * @param ErrorType The type of the error to catch
 */
interface ErrorHandler<ErrorType: Throwable>: BetterKtorEndpoint {
	suspend fun errorHandler(error: ErrorType) {}
	suspend fun errorHandler(error: ErrorType, call: ApplicationCall) {}
}

/**
 * An extendable class for complex websockets providing session injection and
 * reflection based message sending. It is recommended for every complex websocket
 * class to extend it.
 *
 * @param SelfType The type of the current class. Just use the current class name.
 *
 * @see Inject
 */
@Suppress("UNCHECKED_CAST")
abstract class ComplexWebsocket<SelfType: ComplexWebsocket<SelfType>>: BetterKtorEndpoint {
	// Internal variables
	
	// the current app cache
	internal lateinit var cache: BetterKtorCache;
	
	// the send method set by the registerer
	@PublishedApi
	internal lateinit var sendMethod: suspend (data: Any, type: KClass<*>) -> Unit;
	
	/**
	 * All instances of the current complex websocket class. Automatically updated on connection/closing.
	 */
	val instances: List<SelfType>
		get() {
			// if the cache is not set
			if (!::cache.isInitialized) error("BetterKtor is not installed for this endpoint!");
			
			// find the list of the instances by the current type
			return cache.cwInstances[this::class] as List<SelfType>;
		}
	
	/**
	 * The injected session. During an endpoint handler execution, this
	 * property will contain the current session instance.
	 * **Beware**: When using `object` endpoints without the [Mutex] annotation,
	 * this will not work.
	 *
	 * @see Inject
	 */
	@Inject(InjectOption.SESSION)
	protected lateinit var session: DefaultWebSocketServerSession;
	
	/**
	 * The injected call. During an endpoint handler execution, this
	 * property will contain the current call instance.
	 * **Beware**: When using `object` endpoints without the [Mutex] annotation,
	 * this will not work.
	 *
	 * @see Inject
	 */
	@InjectCall
	protected lateinit var call: ApplicationCall;
	
	/**
	 * A method handler for the websocket connection start
	 */
	open suspend fun onConnect() {}
	
	/**
	 * A method handler for the websocket connection close
	 */
	open suspend fun onClose() {}
	
	/**
	 * Sends a message to the websocket
	 */
	suspend inline fun <reified T: Any> send(data: T) {
		sendMethod(data, T::class);
	}
}