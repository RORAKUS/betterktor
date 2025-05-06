package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.internal.other.dropLines
import codes.rorak.betterktor.internal.other.getKey
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal class NormalEndpoint(cache: BetterKtorCache, f: KFunction<*>):
	FunctionEndpoint(cache, f) {
	
	lateinit var returnType: KClass<*>;
	lateinit var httpMethod: HttpMethod;
	var mutex: Mutex? = null;
	
	override fun register() {
		TODO("Not yet implemented");
	}
	
	override fun toString() = """
		${super.toString().dropLines(1)}
		Type: NORMAL
		HTTP method: ${httpMethod.value}
		Mutex id: ${BetterKtor.mutexMap.getKey(mutex)}
		Return type: ${returnType.simpleName}
		================
	""".trimIndent();
}