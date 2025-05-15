package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf

internal abstract class FunctionEndpoint(cache: BetterKtorCache, val function: KFunction<*>): BaseEndpoint(cache) {
	var ignored = false; protected set;
	var parameterTypes: MutableMap<KClass<*>, ParameterGetter> = mutableMapOf(); protected set;
	
	override fun toString() = """
		=== Function endpoint '${function.name}' ===
		${super.toString()}
		Parameter types: ${parameterTypes.keys.joinToString(", ") { it.simpleName!! }}
		================
	""".trimIndent();
	
	data class ParameterGetter(
		val getter: (suspend ApplicationCall.() -> Any?)? = null,
		val wsGetter: (suspend DefaultWebSocketServerSession.() -> Any?)? = null,
		val sseGetter: (suspend ServerSSESession.() -> Any?)? = null,
		val isError: Boolean = false
	) {
		companion object {
			// return the parameter getter for the current class
			fun of(type: KClass<*>, nullable: Boolean, cache: BetterKtorCache) = when {
				// check if a custom receiver is specified
				type in cache.config.customReceivers -> ParameterGetter(cache.config.customReceivers[type]!!);
				// call or session properties
				type == ApplicationCall::class -> ParameterGetter({ this }, { call }, { call });
				type == DefaultWebSocketServerSession::class -> ParameterGetter(wsGetter = { this });
				type == ServerSSESession::class -> ParameterGetter(sseGetter = { this });
				type.isSubclassOf(Throwable::class) -> ParameterGetter(isError = true);
				// a receiver
				else -> ParameterGetter({
					// receive the value and return the value
					runCatching { receive(type) }.getOrElse {
						// if there is an error and the parameter is not nullable, throw
						if (!nullable) throw it;
						// else return null
						null;
					};
				});
			};
		}
	};
}