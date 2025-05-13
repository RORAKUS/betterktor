package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.annotations.*
import codes.rorak.betterktor.api.ComplexWebsocket
import codes.rorak.betterktor.internal.endpoints.ComplexWebsocketEndpoint
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import codes.rorak.betterktor.internal.other.isOverridenFrom
import codes.rorak.betterktor.internal.other.removeFirst
import codes.rorak.betterktor.util.BetterKtorError
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters

internal class ComplexWebsocketResolver(
	val cache: BetterKtorCache,
	val clazz: KClass<*>,
	val outerClass: EndpointClass?
) {
	// the endpoint
	private val endpoint = ComplexWebsocketEndpoint(cache, clazz, outerClass);
	
	/**
	 * Resolving a complex websockets:
	 * 1. Resolve the path
	 *      - name = if strict mode off, no annotation, no override and name matches -> select the matching group
	 * 2. Resolve the mutex
	 * 3. Resolve the auth
	 * 4. Go through the methods, resolve them
	 *      * 1x onConnect, onClose
	 *      * onError, onMessage, onMessageSend
	 *      - mutex
	 *      - connect, close -> override -> annotation ->? name
	 *      - the rest -> annotation ->? name
	 *      / check the parameters
	 *      / onMessageSend -> check the return type and the parameter type
	 * 5. Go through the properties and find flows
	 */
	
	fun resolve(): ComplexWebsocketEndpoint {
		processPath();
		endpoint.auth = CommonProcessor.authProcessor(clazz, outerClass, cache);
		endpoint.mutex = CommonProcessor.mutexProcessor(clazz, outerClass?.mutex, cache);
		processMethods();
		processProperties();
		
		return endpoint;
	}
	
	private fun processPath() {
		// set the real name of the class endpoint
		var realName = clazz.simpleName!!;
		
		// if the class could be a resolved endpoint
		if (
			!cache.strict &&
			!clazz.hasAnnotation<codes.rorak.betterktor.annotations.ComplexWebsocket>() &&
			!clazz.isSubclassOf(ComplexWebsocket::class)
		) {
			// match the name to the complex websocket regex
			val nameMatch = cache.naming.complexWebsocket.matchEntire(realName);
			// if the match was successful, set the real name
			if (nameMatch != null) realName = nameMatch.groupValues[1];
		}
		
		// use the common processor to get the path
		val (path, _) = CommonProcessor.pathProcessor(
			element = clazz,
			parent = outerClass,
			cache = cache,
			packageName = clazz.java.packageName,
			name = realName
		);
		
		// set the endpoint path variable
		endpoint.path = path;
	}
	
	private fun processMethods() = clazz.declaredMemberFunctions.forEach { f ->
		cache.current(f);
		
		// skip the method if ignored
		if (f.hasAnnotation<Ignore>()) return@forEach;
		
		// get the type of the method, or continue if none
		val type = resolveMethodType(f) ?: return@forEach;
		
		// there can be only one onConnect & onClose method
		if (type == MethodType.CONNECT && endpoint.onConect != null)
			throw BetterKtorError("There can be only one connect handler for a complex websocket!", cache);
		if (type == MethodType.CLOSE && endpoint.onClose != null)
			throw BetterKtorError("There can be only one close handler for a complex websocket!", cache);
		
		// resolve the mutex of the method
		val mutex = CommonProcessor.mutexProcessor(f, endpoint.mutex, cache);
		
		// resolve the parameter types --> map it to a KClass instance or throw
		val parameters = f.valueParameters.map {
			it.type.classifier as? KClass<*> ?: throw BetterKtorError("Parameter '${it.name}': Invalid type!", cache);
		};
		
		// validate the parameter count & types
		type.validate(parameters, cache);
		
		// enforce the same return type as the parametere for the message send type
		if (type == MethodType.MESSAGE_SEND) {
			// get and cast the return type
			val returnType = f.returnType.classifier as? KClass<*>
				?: throw BetterKtorError("Invalid return type!", cache);
			
			// compare it to the distinct type
			if (returnType != type.distinctType)
				throw BetterKtorError(
					"The message send handler must return the same type as its parameter type!",
					cache
				);
		}
		
		// create the function object
		val functionObject = ComplexWebsocketEndpoint.FunctionObject(f, parameters, type.distinctType, mutex);
		
		// set the method variable in the endpoint
		when (type) {
			MethodType.CONNECT -> endpoint.onConect = functionObject;
			MethodType.CLOSE -> endpoint.onClose = functionObject;
			MethodType.ERROR -> endpoint.onError += functionObject;
			MethodType.MESSAGE -> endpoint.onMessage += functionObject;
			MethodType.MESSAGE_SEND -> endpoint.onMessageSend += functionObject;
		}
	};
	
	@Suppress("UNCHECKED_CAST")
	private fun processProperties() = clazz.declaredMemberProperties.forEach { prop ->
		cache.current(prop);
		
		// skip if ignored
		if (prop.hasAnnotation<Ignore>()) return@forEach;
		
		// check if the property is a flow
		/**
		 * Logic table:
		 * A S N | !A && (!N || S)
		 * ---------
		 * 0 0 0 | 1
		 * 0 0 1 | 0
		 * 0 1 0 | 1
		 * 0 1 1 | 1
		 * 1 0 0 | 0
		 * 1 0 1 | 0
		 * 1 1 0 | 0
		 * 1 1 1 | 0
		 */
		if (!prop.hasAnnotation<CWFlow>() && (!cache.naming.complexWebsocketFlow.matches(prop.name) || cache.strict))
			return@forEach;
		
		// check the type of the flow
		val type = prop.returnType.classifier as? KClass<*>
			?: throw BetterKtorError("Invalid type!", cache);
		if (type != MutableSharedFlow::class) throw BetterKtorError("The type must be 'MutableSharedFlow'!", cache);
		
		// set the flow object
		endpoint.flowObjects += prop as KMutableProperty1<*, MutableSharedFlow<*>>;
	};
	
	private fun resolveMethodType(method: KFunction<*>): MethodType? {
		// overriden type -> annotation type -> name type
		
		// check the method for possible overrides
		if (method.isOverridenFrom(ComplexWebsocket<*>::onConnect)) return MethodType.CONNECT;
		if (method.isOverridenFrom(ComplexWebsocket<*>::onClose)) return MethodType.CLOSE;
		
		// check the method for an annotation
		if (method.hasAnnotation<CWOnConnect>()) return MethodType.CONNECT;
		if (method.hasAnnotation<CWOnClose>()) return MethodType.CLOSE;
		if (method.hasAnnotation<CWOnError>()) return MethodType.ERROR;
		if (method.hasAnnotation<CWOnMessage>()) return MethodType.MESSAGE;
		if (method.hasAnnotation<CWOnMessageSend>()) return MethodType.MESSAGE_SEND;
		
		// if the strict mode is on, return null
		if (cache.strict) return null;
		
		// check the method for a name match
		with(cache.naming) {
			if (complexWebsocketOnConnect.matches(method.name)) return MethodType.CONNECT;
			if (complexWebsocketOnClose.matches(method.name)) return MethodType.CLOSE;
			if (complexWebsocketOnError.matches(method.name)) return MethodType.ERROR;
			if (complexWebsocketOnMessage.matches(method.name)) return MethodType.MESSAGE;
			if (complexWebsocketOnMessageSend.matches(method.name)) return MethodType.MESSAGE_SEND;
		};
		
		// if the type was not resolved, return null
		return null;
	}
	
	private enum class MethodType(private vararg val allowedTypes: Pair<KClass<*>, Boolean>) {
		CONNECT(DefaultWebSocketServerSession::class to false, ApplicationCall::class to false),
		CLOSE(DefaultWebSocketServerSession::class to false),
		ERROR(DefaultWebSocketServerSession::class to false, Throwable::class to true),
		MESSAGE(DefaultWebSocketServerSession::class to false, Any::class to true),
		MESSAGE_SEND(DefaultWebSocketServerSession::class to false, Any::class to true);
		
		lateinit var distinctType: KClass<*>;
		
		fun validate(params: List<KClass<*>>, cache: BetterKtorCache) {
			val parameters = params.toMutableList();
			
			// get the optional and required parameters
			val optionalParameters = allowedTypes.filterNot { it.second }.map { it.first };
			val requiredParameters = allowedTypes.filter { it.second }.map { it.first };
			
			// remove the optional parameters from the list -> only required and illegal parameters remain
			//  (removes only one instance -> optional parameters cannot be duplicate)
			optionalParameters.forEach { parameters.remove(it) };
			
			// check if the required parameters are present, then remove them
			requiredParameters.forEach { rp ->
				// a help variable for the checking lambda
				val subclassCheck = { p: KClass<*> -> p.isSubclassOf(rp); }
				
				// check if the parameter is present
				if (!parameters.any(subclassCheck))
					throw BetterKtorError("A required parameter of type '${rp.simpleName}' is missing!", cache);
				
				// set the distinct type
				distinctType = parameters.first(subclassCheck);
				
				// remove it from the list
				parameters.removeFirst(subclassCheck);
			};
			
			// if the parameter list is not empty, illegal parameters are present
			if (parameters.isNotEmpty())
				throw BetterKtorError("Invalid parameters found! Types: '${parameters.joinToString()}'", cache);
		}
	}
}