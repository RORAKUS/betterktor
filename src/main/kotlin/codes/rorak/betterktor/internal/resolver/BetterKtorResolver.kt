package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.internal.endpoints.BaseEndpoint
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import codes.rorak.betterktor.internal.other.ClassLoader
import codes.rorak.betterktor.internal.other.EndpointType
import codes.rorak.betterktor.internal.other.debug
import codes.rorak.betterktor.internal.other.dotJoin
import codes.rorak.betterktor.internal.other.log
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions

internal class BetterKtorResolver(val cache: BetterKtorCache) {
	fun resolve() {
		debug("Resolving endpoints...");
		
		// get the list of all packages, where the endpoints could be
		val packages = getEndpointsPackages();
		
		// load all classes and functions inside the packages
		val loader = ClassLoader(packages);
		loader.load(strict = cache.config.strict);
		
		// resolve all classes
		loader.classes().forEach { resolveClass(it); };
		// resolve all functions
		loader.functions().forEach { resolveFunction(it); };
		
		debug("Endpoints successfully resolved!");
	}
	
	private fun resolveClass(clazz: KClass<*>, outerClass: EndpointClass? = null) {
		debug("Resolving class: ${clazz.simpleName}...");
		cache.current(clazz);
		
		// resolve the endpoint
		val endpointClass = EndpointClassResolver(cache, clazz, outerClass).resolve();
		
		// check if the endpoint is valid
		if (endpointClass.ignored) {
			debug("Ignored");
			return;
		}
		
		//  check if the class is a complex websocket
		if (endpointClass.defaultType == EndpointType.COMPLEX_WEBSOCKET) {
			resolveComplexWebsocket(clazz, outerClass);
			return;
		}
		
		debug("Resolved a class endpoint");
		debug(endpointClass.toString());
		
		// process all methods
		clazz.declaredMemberFunctions.forEach {
			resolveFunction(it, endpointClass);
		};
		
		// process all nested non-inner classes
		clazz.nestedClasses.filterNot { it.isInner && !it.java.isInterface && !it.java.isEnum }.forEach {
			// check for only one level
			if (outerClass != null) {
				log.warn("${cache.errorMeta()} - ${it.simpleName!!}: Second level nested classes won't be recognized!");
				return@forEach;
			}
			
			resolveClass(it, endpointClass);
		};
		
		debug("Resolved class: ${clazz.simpleName}");
	}
	
	private fun resolveFunction(function: KFunction<*>, clazz: EndpointClass? = null) {
		debug("Resolving function: ${function.name}...");
		cache.current(function);
		
		// resolve the endpoint
		val endpoint = EndpointResolver(cache, function, clazz).resolve();
		
		// check if the endpoint is valid
		if (endpoint.ignored) {
			debug("Ignored");
			return;
		}
		
		// add the endpoint
		cache.endpoints += endpoint;
		
		debug("Resolved function: ${function.name}");
		debug(endpoint.toString());
	}
	
	private fun resolveComplexWebsocket(clazz: KClass<*>, outerClass: EndpointClass?) {
		debug("Class was found to be a complex websocket, resolving...");
		
		// resolve the endpoint
		val endpoint = ComplexWebsocketResolver(cache, clazz, outerClass).resolve();
		
		// add the endpoint
		cache.endpoints += endpoint;
		
		debug("Resolved complex websocket!");
		debug(endpoint.toString());
	}
	
	// for every endpoint package
	private fun getEndpointsPackages() = cache.config.endpointsPackages.map {
		// join it with the base package and use a dot if both are not empty
		cache.config.basePackage dotJoin it;
	};
	
	fun register() {
		debug("Registering endpoints...");
		
		cache.endpoints.forEach(BaseEndpoint::register);
		
		debug("Endpoints successfully registered!");
	}
}