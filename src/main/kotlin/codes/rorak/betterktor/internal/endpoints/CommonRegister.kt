package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.annotations.AuthOption
import codes.rorak.betterktor.annotations.InjectOption
import codes.rorak.betterktor.internal.other.EndpointClassInfo
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

internal object CommonRegister {
	// creates an instance or uses an instance object, sets injected properties
	fun handleInstance(classInfo: EndpointClassInfo, sessionOrCall: Any, cache: BetterKtorCache): Any {
		// get the instance -> either the object instance or a new one
		val instance = classInfo.clazz.objectInstance ?: classInfo.clazz.createInstance();
		
		// inject the properties
		classInfo.injectedProperties.forEach { (prop, option) ->
			// get the type of the property
			val datatype = prop.returnType.classifier as KClass<*>;
			// get the parameter
			var parameter = option.second;
			
			// handle special parameter case -> principal auth id
			if (option.first == InjectOption.PRINCIPAL) parameter = parameter ?: cache.config.defaultAuthId;
			
			// get the value -> call the getter according to the type
			val value = when (sessionOrCall) {
				is ApplicationCall -> option.first.getter(sessionOrCall, parameter to datatype);
				is DefaultWebSocketServerSession -> option.first.wsGetter(sessionOrCall, parameter to datatype);
				is ServerSSESession -> option.first.sseGetter(sessionOrCall, parameter to datatype);
				else -> throw IllegalStateException();
			};
			
			// set the property to the option
			prop.setter.call(instance, value);
		};
		
		// return the instance
		return instance;
	}
	
	// if auth is defined, it uses it. If not, it calls the function normally
	fun optionalAuth(authObject: Pair<String, AuthOption>?, routing: Routing, routeBlock: Route.() -> Unit) =
		with(routing) {
			if (authObject != null)
				authenticate(authObject.first, strategy = authObject.second.strategy, build = routeBlock);
			else
				routeBlock();
		};
	
	// if the mutex is defined, it uses it. If not, it calls the function normally
	suspend fun <T> optionalMutex(mutex: Mutex?, block: suspend () -> T): T =
		if (mutex != null) mutex.withLock { block() } else block();
}