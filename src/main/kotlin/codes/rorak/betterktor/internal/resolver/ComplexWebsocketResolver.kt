package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.internal.endpoints.ComplexWebsocketEndpoint
import codes.rorak.betterktor.internal.endpoints.EndpointClass
import kotlin.reflect.KClass

internal class ComplexWebsocketResolver(
	val cache: BetterKtorCache,
	val clazz: KClass<*>,
	val outerClass: EndpointClass?
) {
	fun resolve(): ComplexWebsocketEndpoint {
		TODO();
	}
}