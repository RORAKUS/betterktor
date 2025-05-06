package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.other.dropLines
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal class ErrorHandlerEndpoint(cache: BetterKtorCache, f: KFunction<*>):
	FunctionEndpoint(cache, f) {
	lateinit var errorType: KClass<*>;
	
	override fun register() {
		TODO("Not yet implemented");
	}
	
	override fun toString() = """
		${super.toString().dropLines(1)}
		Error type: ${errorType.simpleName}
		================
	""".trimIndent();
}