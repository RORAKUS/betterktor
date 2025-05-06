package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal abstract class FunctionEndpoint(cache: BetterKtorCache, val function: KFunction<*>): BaseEndpoint(cache) {
	var ignored = false; protected set;
	var parameterTypes: MutableList<KClass<*>> = mutableListOf(); protected set;
	
	override fun toString() = """
		=== Function endpoint '${function.name}' ===
		${super.toString()}
		Parameter types: ${parameterTypes.joinToString(", ") { it.simpleName!! }}
		================
	""".trimIndent();
}