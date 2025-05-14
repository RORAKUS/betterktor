package codes.rorak.betterktor.annotations

import codes.rorak.betterktor.BetterKtorConfig
import codes.rorak.betterktor.util.Casing
import codes.rorak.betterktor.util.CasingMethod
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import io.ktor.util.*
import kotlin.reflect.KClass

/**
 * Wrapper for [HttpMethod], which can be used in annotations
 * @see Method
 */
enum class HttpMethodOption {
	/**
	 * All calls will be directed to the endpoint
	 */
	ANY,
	GET,
	HEAD,
	POST,
	PUT,
	DELETE,
	OPTIONS,
	PATCH;
	
	/**
	 * Returns a ktor usable [HttpMethod] instance
	 */
	val ktor = HttpMethod(name);
}

/**
 * Options for path relativity for an endpoint
 * @see Path
 * @see RelativeTo
 */
enum class RelativeOption {
	/**
	 * The default behaviour for [Path] annotation: string path with special formatting will be used.
	 *
	 * The default behaviour for [RelativeTo] annotation: the endpoint will be relative to it's closest parent
	 *
	 * Available string formats:
	 * - `/...` - works like [NONE]
	 * - `...` - works like [CLASS] for methods and inner classes and [PACKAGE] for classes and function endpoints
	 * - `?/...` - works like [PACKAGE] for methods and [CLASS] for function endpoints, doesn't work for classes
	 * - `$/...` - works like [ROOT_PATH]
	 */
	DEFAULT,
	
	/**
	 * The path will be relative to the current class name or file name for function endpoints
	 */
	CLASS,
	
	/**
	 * The path will be relative to the current package path
	 */
	PACKAGE,
	
	/**
	 * The path will be relative to the root path set in the configuration
	 * @see BetterKtorConfig.rootPath
	 */
	ROOT_PATH,
	
	/**
	 * The path will be absolute
	 */
	NONE;
}

/**
 * The option for the [Auth] annotation. Determines if the authetication should be
 * required, optional or none.
 */
enum class AuthOption {
	/**
	 * The authentication for the endpoint is required
	 */
	REQUIRED,
	
	/**
	 * The authentication for the endpoint is optional
	 */
	OPTIONAL,
	
	/**
	 * There is no authentication for the endpoint.
	 * Used to cancel whole-class authentication (a whole class annotated with [Auth])
	 */
	NONE;
	
	internal val strategy
		get() = when (this) {
			REQUIRED -> AuthenticationStrategy.Required
			OPTIONAL -> AuthenticationStrategy.Optional
			NONE -> throw IllegalStateException();
		};
}

/**
 * Options for the `@Casing` annotation
 *
 * @see codes.rorak.betterktor.annotations.Casing
 */
enum class CasingOption(val method: CasingMethod) {
	/**
	 * Uses the default casing specified in the configuration
	 */
	DEFAULT({ "" }),
	
	/**
	 * All names of the endpoint are converted to `camelCase`
	 */
	CAMEL_CASE(Casing.camelCase),
	
	/**
	 * All names of the endpoint are converted to `snake_case`
	 */
	SNAKE_CASE(Casing.snakeCase),
	
	/**
	 * All names of the endpoint are converted to `kebab-case`
	 */
	KEBAB_CASE(Casing.kebabCase),
	
	/**
	 * All names of the endpoint are converted to `PascalCase`
	 */
	PASCAL_CASE(Casing.pascalCase),
	
	/**
	 * All names of the endpoint are converted to `Train-Case`
	 */
	TRAIN_CASE(Casing.trainCase);
}

/**
 * An option for the [ErrorHandler] annotation.
 * Specifies the type of error handler to use.
 */
enum class ErrorHandlerOption {
	/**
	 * Default behaviour. Depending on resolver config it will firstly
	 * try to determine the type using the method's parameters and if that is
	 * unsuccessful, it will use the [NORMAL] option.
	 */
	DEFAULT,
	
	/**
	 * The error handler acts as a normal endpoint error handler
	 */
	NORMAL,
	
	/**
	 * The error handler acts as a websocket error handler
	 */
	WEBSOCKET,
	
	/**
	 * The error handler acts as a SSE error handler
	 */
	SSE;
}

/**
 * Options for the [Inject] annotation
 */
enum class InjectOption(
	internal val getter: ApplicationCall.(p: Pair<String?, KClass<*>>) -> Any? = {},
	internal vararg val allowedTypes: KClass<*> = arrayOf(Any::class),
	internal val wsGetter: DefaultWebSocketServerSession.(p: Pair<String?, KClass<*>>) -> Any? = { call.getter(it) },
	internal val sseGetter: ServerSSESession.(p: Pair<String?, KClass<*>>) -> Any? = { call.getter(it) },
	internal val parameter: Parameter = Parameter.NONE,
	internal val types: List<Type> = Type.entries,
) {
	/**
	 * Inject the [ApplicationCall].
	 * Works for requests, websockets, SSEs and error handlers.
	 *
	 * **Value type:** [ApplicationCall]
	 */
	CALL({ this }, ApplicationCall::class),
	
	/**
	 * Inject the [ApplicationCall.request].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [ApplicationRequest]
	 */
	REQUEST({ request }, ApplicationRequest::class),
	
	/**
	 * Inject the [ApplicationCall.response].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [ApplicationResponse]
	 */
	RESPONSE({ response }, ApplicationResponse::class),
	
	/**
	 * Inject the [ApplicationCall.application] - the current application.
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [Application]
	 */
	APPLICATION({ application }, Application::class),
	
	/**
	 * Inject the [DefaultWebSocketServerSession] for websockets or [ServerSSESession] for SSEs.
	 * Works for websockets and SSEs.
	 *
	 * **Value type:** [DefaultWebSocketServerSession] or [ServerSSESession]
	 */
	SESSION(
		{}, DefaultWebSocketServerSession::class, ServerSSESession::class,
		wsGetter = { this }, sseGetter = { this },
		types = listOf(Type.WEBSOCKET, Type.SSE)
	),
	
	
	/**
	 * Inject the [ApplicationCall.parameters].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [Parameters]
	 */
	PARAMETERS({ parameters }, Parameters::class),
	
	/**
	 * Inject the [ApplicationCall.attributes].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [Attributes]
	 */
	ATTRIBUTES({ attributes }, Attributes::class),
	
	/**
	 * Inject the [ApplicationRequest.cookies] from [ApplicationCall.request].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [RequestCookies]
	 */
	COOKIES({ request.cookies }, RequestCookies::class),
	
	/**
	 * Inject the [ApplicationRequest.queryParameters] from [ApplicationCall.request].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [Parameters]
	 */
	QUERY_PARAMS({ request.queryParameters }, Parameters::class),
	
	/**
	 * Inject the [ApplicationRequest.headers] from [ApplicationCall.request].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [Headers]
	 */
	HEADERS({ request.headers }, Headers::class),
	
	
	/**
	 * Inject the [ApplicationResponse.cookies] from [ApplicationCall.response].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [ResponseCookies]
	 */
	RESPONSE_COOKIES({ response.cookies }, ResponseCookies::class, types = listOf(Type.REQUEST)),
	
	/**
	 * Inject the [ApplicationResponse.headers] from [ApplicationCall.response].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [ResponseHeaders]
	 */
	RESPONSE_HEADERS({ response.headers }, ResponseHeaders::class, types = listOf(Type.REQUEST)),
	
	/**
	 * Inject the [ApplicationCall.principal] with the type of the property.
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** any
	 *
	 * **Parameter:** optional, the name of the auth principal. Default value is set in [BetterKtorConfig.defaultAuthId]
	 */
	PRINCIPAL({ authentication.principal(it.first!!, it.second) }, parameter = Parameter.OPTIONAL),
	
	
	/**
	 * Inject the selected attribute from [ApplicationCall.attributes] with the type that matches the type of the property.
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** any
	 *
	 * **Parameter:** The name of the attribute
	 */
	ATTRIBUTE({ attributes[AttributeKey<Any>(it.first!!)] }, parameter = Parameter.REQUIRED),
	
	/**
	 * Inject the selected parameter from [ApplicationCall.parameters].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [String]
	 *
	 * **Parameter:** The name of the parameter
	 */
	PARAMETER({ parameters[it.first!!] }, String::class, parameter = Parameter.REQUIRED),
	
	/**
	 * Inject the selected cookie from [ApplicationRequest.cookies].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [String]
	 *
	 * **Parameter:** The name of the cookie
	 */
	COOKIE({ request.cookies[it.first!!] }, String::class, parameter = Parameter.REQUIRED),
	
	/**
	 * Inject the selected query parameter from [ApplicationRequest.queryParameters].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [String]
	 *
	 * **Parameter:** The name of the parameter
	 */
	QUERY_PARAM({ request.queryParameters[it.first!!] }, String::class, parameter = Parameter.REQUIRED),
	
	/**
	 * Inject the selected header from [ApplicationRequest.headers].
	 * Works for requests, websockets and SSEs.
	 *
	 * **Value type:** [String]
	 *
	 * **Parameter:** The name of the header
	 */
	HEADER({ request.headers[it.first!!] }, String::class, parameter = Parameter.REQUIRED);
	
	internal enum class Type {
		REQUEST, SSE, WEBSOCKET;
	}
	
	internal enum class Parameter {
		NONE, OPTIONAL, REQUIRED;
	}
}