package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

internal class WebsocketEndpoint(cache: BetterKtorCache, f: KFunction<*>):
	FunctionEndpoint(cache, f) {
	
	override fun register() {
		// instantiation, parameters, auth, injection
		cache.application.routing {
			// if authentication is required, use it
			CommonRegister.optionalAuth(auth, this) {
				// register the websocket route
				route(path.render()) {
					webSocket {
						// get the instance if the function has a parent class
						//  (it also injects the properties)
						val instance = classInfo?.let { CommonRegister.handleInstance(it, this, cache) };
						// receive the parameters
						val parameters = parameterTypes.map { it.value.wsGetter!!.invoke(this) }.toMutableList();
						// if the instance is not null, add it to the start of the parameters
						if (instance != null) parameters.add(0, instance);
						
						// call the provided function
						function.callSuspend(parameters);
					};
				};
			};
		};
	}
}