package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.other.suspendCall
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlin.reflect.KFunction

internal class SSEEndpoint(cache: BetterKtorCache, f: KFunction<*>):
	FunctionEndpoint(cache, f) {
	
	override fun register() {
		cache.application.routing {
			// if auth is not null, use it
			CommonRegister.optionalAuth(auth, this) {
				// register the SSE route
				route(path.render()) {
					sse {
						// get the instance if the function has a parent class
						//  (it also injects the properties)
						val instance = classInfo?.let { CommonRegister.handleInstance(it, this, cache) };
						// receive the parameters
						val parameters = parameterTypes.map { it.value.sseGetter!!.invoke(this) }.toMutableList();
						
						// call the provided function
						function.suspendCall(instance, parameters);
					};
				};
			};
		};
	}
}