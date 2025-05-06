package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import kotlin.reflect.KFunction

internal class SSEEndpoint(cache: BetterKtorCache, f: KFunction<*>):
	FunctionEndpoint(cache, f) {
	
	override fun register() {
		TODO("Not yet implemented");
	}
}