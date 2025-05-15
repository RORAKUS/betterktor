package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.annotations.*
import codes.rorak.betterktor.api.BetterKtorEndpoint
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import codes.rorak.betterktor.internal.other.ANY_CALL_METHOD
import codes.rorak.betterktor.internal.other.EndpointType
import codes.rorak.betterktor.internal.other.Interrupt
import codes.rorak.betterktor.internal.other.isCWType
import codes.rorak.betterktor.internal.other.isMethodAnnotation
import codes.rorak.betterktor.internal.other.isTypeAnnotation
import codes.rorak.betterktor.util.BetterKtorError
import io.ktor.http.*
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

internal class EndpointClassResolver(val cache: BetterKtorCache, val clazz: KClass<*>, val outerClass: EndpointClass?) {
	// the endpoint
	private val endpoint = EndpointClass(cache, clazz);
	
	// the type from an interface, then the final type
	private var interfaceDefaultType: EndpointType? = null;
	
	// the type from an annotation
	private var defaultType: EndpointType? = null;
	
	// the default http method
	private var defaultHttpMethod: HttpMethod? = null;
	
	fun resolve() = runCatching {
		checkIgnore();
		processDefaultTypeAndMethod();
		processPath();
		endpoint.auth = CommonProcessor.authProcessor(clazz, outerClass, cache);
		endpoint.mutex = CommonProcessor.mutexProcessor(clazz, outerClass?.mutex, cache);
		endpoint.injectedProperties = CommonProcessor.injectedPropertiesProcessor(clazz, cache);
		
		return@runCatching endpoint;
	}.getOrElse {
		// Handle only interrupt errors
		if (it !is Interrupt) throw it;
		// if the interrupt wasn't because of a complex websocket type, set the ignore property
		if (endpoint.defaultType != EndpointType.COMPLEX_WEBSOCKET) endpoint.ignored = true;
		return@getOrElse endpoint;
	};
	
	private fun checkIgnore() {
		// if annotation ignored, it is always ignored
		if (clazz.hasAnnotation<Ignore>()) throw Interrupt;
		
		// if in normal mode (not strict) and the name matches the ignored name regex
		if (!cache.strict && cache.naming.ignored.matches(clazz.simpleName!!))
			throw Interrupt;
	}
	
	private fun processDefaultTypeAndMethod() {
		// type: annotation type -> interface type -> parent type -> default type
		// method: annotation -> default
		
		processAnnotations();
		processInterfaceTypes();
		
		// set the type to interface type, if there is no annotation type
		if (defaultType == null) defaultType = interfaceDefaultType;
		
		// if the strict mode is off, the type is null and the name matches a complex websocket name, set it as complex websocket
		if (!cache.strict && defaultType == null && cache.naming.complexWebsocket.matches(clazz.simpleName!!))
			defaultType = EndpointType.COMPLEX_WEBSOCKET;
		
		// interrupt for complex websocket
		if (defaultType == EndpointType.COMPLEX_WEBSOCKET) throw Interrupt;
		// if the type is null and strict mode is on, interrupt
		if (cache.strict && defaultType == null) throw Interrupt;
		
		// if the type is null, set it to parent type, or default
		if (defaultType == null)
			defaultType = outerClass?.defaultType ?: EndpointType.ENDPOINT;
		
		// if the default http method is null, set it to parent type or default
		if (defaultHttpMethod == null)
			defaultHttpMethod = outerClass?.defaultHttpMethod ?: cache.config.defaultNamedRouteMethod;
		
		// set the endpoint object values
		endpoint.defaultType = defaultType!!;
		endpoint.defaultHttpMethod = defaultHttpMethod!!;
	}
	
	private fun processPath() {
		// use the common processor to get the path
		val (path, casing) = CommonProcessor.pathProcessor(
			element = clazz,
			parent = outerClass,
			cache = cache,
			packageName = clazz.java.packageName,
			name = clazz.simpleName!!
		);
		
		// set the endpoint object values
		endpoint.path = path;
		endpoint.casing = casing;
	}
	
	private fun processAnnotations() = clazz.annotations.forEach { a ->
		// an endpoint cannot have multiple type annotations
		if (a.isTypeAnnotation && defaultType != null)
			throw BetterKtorError("An endpoint class cannot have multiple default type annotations!", cache);
		// an endpoint cannot have multiple default method annotations
		if (a.isMethodAnnotation && defaultHttpMethod != null)
			throw BetterKtorError("An endpoint class cannot have multiple default method annotations!", cache);
		
		when (a) {
			// types
			is Endpoint -> defaultType = EndpointType.ENDPOINT;
			is Websocket -> defaultType = EndpointType.WEBSOCKET;
			is SSE -> defaultType = EndpointType.SSE;
			is ErrorHandler -> defaultType = EndpointType.ERROR_HANDLER;
			is ComplexWebsocket -> defaultType = EndpointType.COMPLEX_WEBSOCKET;
			// methods
			is AnyCall -> defaultHttpMethod = ANY_CALL_METHOD;
			is Get -> defaultHttpMethod = HttpMethod.Get;
			is Post -> defaultHttpMethod = HttpMethod.Post;
			is Put -> defaultHttpMethod = HttpMethod.Put;
			is Delete -> defaultHttpMethod = HttpMethod.Delete;
			is Patch -> defaultHttpMethod = HttpMethod.Patch;
			// if the annotation has a custom method definition, set the custom method
			is Method -> defaultHttpMethod = if (a.custom.isNotEmpty()) HttpMethod(a.custom) else a.method.ktor;
		}
	}.also {
		// a complex websocket cannot have method annotations
		if (defaultType == EndpointType.COMPLEX_WEBSOCKET && defaultHttpMethod != null)
			throw BetterKtorError("A complex websocket class cannot have a default http method!", cache);
	}
	
	private fun processInterfaceTypes() = clazz.allSuperclasses.forEach { c ->
		// the class cannot be a complex websocket and a normal endpoint at the same time
		if (
		// current == CWS && type != null|CWS
			(c.isCWType && defaultType != null && defaultType != EndpointType.COMPLEX_WEBSOCKET) ||
			// current == CWS && interfaceType != null
			(c.isCWType && interfaceDefaultType != null) ||
			// current == NORMAL && type == CWS
			(c.isSubclassOf(BetterKtorEndpoint::class) && defaultType == EndpointType.COMPLEX_WEBSOCKET) ||
			// current == NORMAL && interfaceType == CWS
			(c.isSubclassOf(BetterKtorEndpoint::class) && interfaceDefaultType == EndpointType.COMPLEX_WEBSOCKET)
		) throw BetterKtorError(
			"An endpoint class cannot be a complex websocket and a normal endpoint at the same time!", cache
		);
		
		// set the variable depending on the implemented type
		when (c) {
			codes.rorak.betterktor.api.Endpoint::class -> interfaceDefaultType = EndpointType.ENDPOINT;
			codes.rorak.betterktor.api.Websocket::class -> interfaceDefaultType = EndpointType.WEBSOCKET;
			codes.rorak.betterktor.api.SSE::class -> interfaceDefaultType = EndpointType.SSE;
			codes.rorak.betterktor.api.ErrorHandler::class -> interfaceDefaultType = EndpointType.ERROR_HANDLER;
			codes.rorak.betterktor.api.ComplexWebsocket::class -> interfaceDefaultType = EndpointType.COMPLEX_WEBSOCKET;
		}
	}
}