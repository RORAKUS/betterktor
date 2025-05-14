package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.annotations.*
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import codes.rorak.betterktor.internal.endpoints.ErrorHandlerEndpoint
import codes.rorak.betterktor.internal.endpoints.FunctionEndpoint
import codes.rorak.betterktor.internal.endpoints.FunctionEndpoint.ParameterGetter
import codes.rorak.betterktor.internal.endpoints.NormalEndpoint
import codes.rorak.betterktor.internal.endpoints.SSEEndpoint
import codes.rorak.betterktor.internal.endpoints.WebsocketEndpoint
import codes.rorak.betterktor.internal.other.*
import codes.rorak.betterktor.util.BetterKtorError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaMethod

internal class EndpointResolver(val cache: BetterKtorCache, val function: KFunction<*>, val clazz: EndpointClass?) {
	// the endpoint
	private lateinit var endpoint: FunctionEndpoint;
	
	/*
	Resolved properties:
	- Type (Normal, WS, SSE...)
	- Path
	- Auth id and option
	- Parameter types (get back to it - maybe change type to some method type instead of KClass<*>)
	- Class instance and injected properties
	* For a normal endpoint:
		- Mutex
		- HTTP method
		- Return type
	* For an error handler endpoint:
		- The error type
	
	Resolving process:
	0. Check if the endpoint isn't ignored
	1. Resolve the type and the HTTP method
		- Check for all conflicts!
		a. Check the method override if parent class not null
		b. Check the annotations
		c. If strict mode is on and the type is not set, interrupt
		d. Check the type name if strict mode is off and the type isn't already set
		e. Check the method name if strict mode is off and the method isn't already set
		f. Set the type and method according to the parent class
	2. Resolve the path
	3. Resolve auth
	4. Resolve parameter types
	5. Set the parent class info
	6. If a normal endpoint
		a. Resolve mutex
		b. Resolve the return type
	7. If an error handler, set the error type
	
	Internal variables needed:
	- The type from overrides
	- The type from annotations
	- If the method is a named route or not
	- The http method from overrides
	- The http method from annotations
	- The edited name, in case of a special named method
	 */
	
	// the type from an override
	private var overrideType: EndpointType? = null;
	
	// the type from an annotation, then the final type
	private var type: EndpointType? = null;
	
	// the http method from an annotation, then the final http method
	private var httpMethod: HttpMethod? = null;
	
	// the type of the handle annotation
	private var handleType: EndpointType? = null;
	
	// whether the endpoint is a named route
	private var namedRoute: Boolean = false;
	
	// the edited name of the endpoint
	private var editedName: String? = null;
	
	fun resolve() = runCatching {
		// common properties
		checkIgnore();
		processTypeAndMethod();
		processPath();
		endpoint.auth = CommonProcessor.authProcessor(function, clazz, cache);
		processParameterTypes();
		
		// set the endpoint class info
		endpoint.classInfo = clazz?.let { EndpointClassInfo(it.clazz, it.injectedProperties); };
		
		// type specific properties -> normal endpoint
		if (type == EndpointType.ENDPOINT) {
			// cast the endpoint object
			val normalEndpoint = endpoint as NormalEndpoint;
			// set the values
			normalEndpoint.httpMethod = httpMethod!!;
			normalEndpoint.mutex = CommonProcessor.mutexProcessor(function, clazz?.mutex, cache);
			// process the rest
			processReturnType(normalEndpoint);
		};
		
		// type specific properties -> error handler endpoint
		if (type == EndpointType.ERROR_HANDLER) {
			// check if status pages package is installed
			runCatching { cache.application.pluginOrNull(StatusPages) }.onFailure {
				throw BetterKtorError(
					"Cannot use an error handler when StatusPages plugin package is not implemented!", cache
				);
			};
			
			// authentication is not supported for error handlers
			endpoint.auth = null; // --> there could be no direct annotation auth, but the endpoint inherited it
			if (function.hasAnnotation<Auth>())
				throw BetterKtorError("Authentication is not supported for error handlers!", cache);
			
			// cast the endpoint object
			val errorHandlerEndpoint = endpoint as ErrorHandlerEndpoint
			// set the error type
			errorHandlerEndpoint.errorType =
				errorHandlerEndpoint.parameterTypes.keys.first { it.isSubclassOf(Throwable::class) };
			// set the mutex
			errorHandlerEndpoint.mutex = CommonProcessor.mutexProcessor(function, clazz?.mutex, cache);
		};
		
		return@runCatching endpoint;
	}.getOrElse {
		// handle only interrupt errors
		if (it !is Interrupt) throw it;
		// if the process was iterrupted, it means the endpoint has to be ignored
		// -> return a new ignored function endpoint
		return@getOrElse object: FunctionEndpoint(cache, function) {
			override fun register() {}
			
			init {
				ignored = true;
			}
		};
	};
	
	private fun checkIgnore() {
		// if annotation ignored, it is always ignored
		if (function.hasAnnotation<Ignore>()) throw Interrupt;
		
		// if in normal mode (not strict) and the name matches the ignored name regex
		if (!cache.strict && cache.naming.ignored.matches(function.name))
			throw Interrupt;
	}
	
	private fun processTypeAndMethod() {
		// type: override -> annotation -> name? -> parent -> normal
		// method: override -> annotation -> name? -> parent -> default
		
		processOverride();
		processAnnotations();
		
		// if the type was not resolved, set it to the override type
		if (type == null) type = overrideType;
		
		// if the strict mode is off and the type is not set, resolve the name type
		if (!cache.strict && type == null) processNaming();
		
		// if the type is not set and strict mode is on, interrupt
		if (cache.strict && type == null) throw Interrupt;
		
		// use the parent default type if the type is still null (and use a named route)
		if (type == null) {
			type = clazz?.defaultType ?: EndpointType.ENDPOINT;
			namedRoute = true;
		}
		
		// set the parent http method if a method is needed
		if (httpMethod == null && type == EndpointType.ENDPOINT)
			httpMethod = clazz?.defaultHttpMethod ?: cache.config.defaultNamedRouteMethod;
		
		// create the endpoint instance depending on the type
		endpoint = when (type) {
			EndpointType.ENDPOINT -> NormalEndpoint(cache, function);
			EndpointType.WEBSOCKET -> WebsocketEndpoint(cache, function);
			EndpointType.SSE -> SSEEndpoint(cache, function);
			EndpointType.ERROR_HANDLER -> ErrorHandlerEndpoint(cache, function);
			EndpointType.COMPLEX_WEBSOCKET, null -> throw IllegalStateException();
		};
	}
	
	private fun processPath() {
		// use the common processor to get the path
		val (path, _) = CommonProcessor.pathProcessor(
			element = function,
			parent = clazz,
			cache = cache,
			packageName = function.javaMethod!!.declaringClass.packageName,
			name = editedName ?: function.name
		);
		
		// set the endpoint variable
		endpoint.path = path;
	}
	
	private fun processParameterTypes() {
		// set the parameter list
		endpoint.parameterTypes.putAll(function.valueParameters.associate { p ->
			// get the classifier (KClass instance) or throw on error
			val type = p.type.classifier as? KClass<*>
				?: throw BetterKtorError("Parameter '${p.name}' has an invalid type!", cache);
			
			// associate with the getter function
			type to ParameterGetter.of(type, p.type.isMarkedNullable, cache);
		});
		
		// the list cannot contain more than one parameter with the specified types
		if (
			(endpoint.parameterTypes.count { it.key == ApplicationCall::class } > 1) ||
			(endpoint.parameterTypes.count { it.key == ApplicationRequest::class } > 1) ||
			(endpoint.parameterTypes.count { it.key == DefaultWebSocketServerSession::class } > 1) ||
			(endpoint.parameterTypes.count { it.key == ServerSSESession::class } > 1) ||
			(endpoint.parameterTypes.count { it.key.isSubclassOf(Throwable::class) } > 1)
		) throw BetterKtorError("There can be only one call, one request, one session and one error parameter!", cache);
		
		// check if the endpoint type matches the parameter for websockets and SSEs
		if (
			(DefaultWebSocketServerSession::class in endpoint.parameterTypes && type != EndpointType.WEBSOCKET) ||
			(ServerSSESession::class in endpoint.parameterTypes && type != EndpointType.SSE) ||
			(endpoint.parameterTypes.any { it.key.isSubclassOf(Throwable::class) } && type != EndpointType.ERROR_HANDLER)
		) throw BetterKtorError(
			"Invalid paramater type! 'DefaultWebSocketServerSession': only for websockets, 'ServerSSESession': only for SSEs, subclass of 'Throwable': only for error handlers",
			cache
		);
	}
	
	private fun processReturnType(normalEndpoint: NormalEndpoint) {
		// the return type cannot be nullable
		if (function.returnType.isMarkedNullable) throw BetterKtorError("A return type cannot be nullable!", cache);
		
		// get the return type
		val returnType = function.returnType.classifier as? KClass<*>
			?: throw BetterKtorError("Invalid return type!", cache);
		
		// set the return type, if the return type is ignored, set it to unit
		normalEndpoint.returnType = if (function.hasAnnotation<IgnoreReturn>()) Unit::class else returnType;
	}
	
	private fun processOverride() = with(function) {
		// if a normal endpoint method, set the endpoint and the method
		if (isOverridenFrom(codes.rorak.betterktor.api.Endpoint::class)) {
			overrideType = EndpointType.ENDPOINT;
			httpMethod = HttpMethod.parse(function.name.uppercase());
			namedRoute = false;
		}
		
		// if a websocket endpoint method, set the endpoint
		if (isOverridenFrom(codes.rorak.betterktor.api.Websocket::class)) {
			overrideType = EndpointType.WEBSOCKET;
			namedRoute = false;
		}
		
		// if an SSE endpoint method, set the endpoint
		if (isOverridenFrom(codes.rorak.betterktor.api.SSE::class)) {
			overrideType = EndpointType.SSE;
			namedRoute = false;
		}
		
		// if an Error handler endpoint, set the endpoint
		if (isOverridenFrom(codes.rorak.betterktor.api.ErrorHandler::class)) {
			overrideType = EndpointType.ERROR_HANDLER;
			namedRoute = false;
		}
	}
	
	private fun processAnnotations() = function.annotations.forEach { a ->
		// an overriden endpoint cannot be an annotation type, nor an annotation method
		if ((a.isTypeAnnotation || a.isAnyMethodAnnotation || a.isHandleAnnotation) && overrideType != null)
			throw BetterKtorError(
				"An overriden endpoint cannot be annotated with an endpoint type, method or handle type annotation!",
				cache
			);
		
		// an endpoint cannot have multiple type annotations
		if (a.isTypeAnnotation || type != null)
			throw BetterKtorError("An endpoint cannot have multiple types!", cache);
		
		// an endpoint cannot have multiple handle types
		if (a.isHandleAnnotation || handleType != null)
			throw BetterKtorError("An endpoint cannot have multiple handle types!", cache);
		
		// an endpoint cannot have multiple method annotations
		if (a.isAnyMethodAnnotation || httpMethod != null)
			throw BetterKtorError("An endpoint cannot have multiple http methods!", cache);
		
		// if there is an endpoint method annotation (@EndpointGet...) or a handle annotation (@WebsocketHandle...), set a named route
		if (a.isEndpointMethodAnnotation || a.isHandleAnnotation) namedRoute = true;
		
		when (a) {
			// types
			is Endpoint -> type = EndpointType.ENDPOINT;
			is Websocket -> type = EndpointType.WEBSOCKET;
			is SSE -> type = EndpointType.SSE;
			is ErrorHandler -> type = EndpointType.ERROR_HANDLER;
			// methods
			is AnyCall, is EndpointAnyCall -> httpMethod = ANY_CALL_METHOD;
			is Get, is EndpointGet -> httpMethod = HttpMethod.Get;
			is Post, is EndpointPost -> httpMethod = HttpMethod.Post;
			is Put, is EndpointPut -> httpMethod = HttpMethod.Put;
			is Delete, is EndpointDelete -> httpMethod = HttpMethod.Delete;
			is Patch, is EndpointPatch -> httpMethod = HttpMethod.Patch;
			// custom methods, check for custom strings
			is Method -> httpMethod = if (a.custom.isNotEmpty()) HttpMethod(a.custom) else a.method.ktor;
			is EndpointMethod -> httpMethod = if (a.custom.isNotEmpty()) HttpMethod(a.custom) else a.method.ktor;
			// handles
			is WebsocketHandle -> handleType = EndpointType.WEBSOCKET;
			is SSEHandle -> handleType = EndpointType.SSE;
			is ErrorHandlerHandle -> handleType = EndpointType.ERROR_HANDLER;
		}
	}.also {
		// an endpoint cannot have a method annotation with a handle annotation
		if (httpMethod != null && handleType != null)
			throw BetterKtorError("An endpoint cannot be a handle and have a http method together!", cache);
		
		// set the type for an unset endpoint with a http method
		if (httpMethod != null && type == null) type = EndpointType.ENDPOINT;
		
		// set the type for an unset endpoint with a handle type
		if (handleType != null && type == null) type = handleType;
		
		// an endpoint cannot have a different type from the handle type
		if (handleType != null && handleType != type)
			throw BetterKtorError("An endpoint type cannot differ from its handle type!", cache);
		
		// only a normal endpoint can have a method annotation
		if (httpMethod != null && type != EndpointType.ENDPOINT)
			throw BetterKtorError("Only a normal endpoint can have a http method defined!", cache);
	};
	
	private fun processNaming() = with(cache.naming) {
		var match: MatchResult? = null;
		
		// http method
		httpMethods.forEach { (regex, method) ->
			match = regex.matchEntire(function.name);
			if (match == null) return@forEach;
			
			type = EndpointType.ENDPOINT;
			httpMethod = method;
			editedName = match.groupValues.getOrNull(1)?.decapitalize();
			namedRoute = editedName != null;
			
			return;
		};
		
		// any call method
		match = anyMethod.matchEntire(function.name);
		if (match != null) {
			type = EndpointType.ENDPOINT;
			httpMethod = ANY_CALL_METHOD;
			editedName = match.groupValues.getOrNull(1)?.decapitalize();
			namedRoute = editedName != null;
			
			return;
		}
		
		// websocket
		match = websocket.matchEntire(function.name);
		if (match != null) {
			type = EndpointType.WEBSOCKET;
			editedName = match.groupValues.getOrNull(1)?.decapitalize();
			namedRoute = editedName != null;
			
			return;
		}
		
		// sse
		match = sse.matchEntire(function.name);
		if (match != null) {
			type = EndpointType.SSE;
			editedName = match.groupValues.getOrNull(1)?.decapitalize();
			namedRoute = editedName != null;
			
			return;
		}
		
		// error handler
		match = errorHandler.matchEntire(function.name);
		if (match != null) {
			type = EndpointType.ERROR_HANDLER;
			editedName = match.groupValues.getOrNull(1)?.decapitalize();
			namedRoute = editedName != null;
			
			return;
		}
	};
}