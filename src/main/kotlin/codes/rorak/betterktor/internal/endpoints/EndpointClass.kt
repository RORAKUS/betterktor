package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.annotations.AuthOption
import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.internal.other.EndpointType
import codes.rorak.betterktor.internal.other.InjectedProperties
import codes.rorak.betterktor.internal.other.Path
import codes.rorak.betterktor.internal.other.getKey
import codes.rorak.betterktor.internal.other.string
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import codes.rorak.betterktor.util.CasingMethod
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass

internal class EndpointClass(val cache: BetterKtorCache, val clazz: KClass<*>) {
	lateinit var defaultHttpMethod: HttpMethod;
	lateinit var defaultType: EndpointType;
	lateinit var path: Path;
	lateinit var injectedProperties: InjectedProperties;
	lateinit var casing: CasingMethod;
	
	var auth: Pair<String, AuthOption>? = null;
	var mutex: Mutex? = null;
	
	var ignored: Boolean = true;
	
	override fun toString() = """
		=== Endpoint class '${clazz.simpleName!!}' ===
		Type: $defaultType
		Path: ${path.render()}
		Method: $defaultHttpMethod
		Casing: $casing
		Auth: $auth
		Mutex id: ${BetterKtor.mutexMap.getKey(mutex)}
		Injected properties: ${injectedProperties.string}
		================
	""".trimIndent();
}