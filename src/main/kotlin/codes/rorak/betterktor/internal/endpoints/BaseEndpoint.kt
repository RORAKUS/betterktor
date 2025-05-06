package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.annotations.AuthOption
import codes.rorak.betterktor.internal.other.EndpointClassInfo
import codes.rorak.betterktor.internal.other.Path
import codes.rorak.betterktor.internal.resolver.BetterKtorCache

internal abstract class BaseEndpoint(protected val cache: BetterKtorCache) {
	lateinit var path: Path;
	var auth: Pair<String, AuthOption>? = null;
	var classInfo: EndpointClassInfo? = null;
	
	abstract fun register();
	
	override fun toString() = """
		Path: ${path.render()}
		Auth: $auth
		Parent class: ${classInfo?.clazz?.simpleName}
	""".trimIndent();
}