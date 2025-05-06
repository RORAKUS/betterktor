package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

internal class ComplexWebsocketEndpoint(cache: BetterKtorCache, val clazz: KClass<*>, val outerClass: EndpointClass?):
	BaseEndpoint(cache) {
	lateinit var onConect: FunctionObject;
	lateinit var onClose: FunctionObject;
	lateinit var onError: FunctionObject;
	
	var onMessageSend = mutableListOf<FunctionObject>();
	var onMessageReceive = mutableListOf<FunctionObject>();
	var flowObjects = mutableListOf<KProperty1<*, *>>();
	
	override fun register() {
		TODO("Not yet implemented");
	}
	
	data class FunctionObject(
		val function: KFunction<*>,
		val parameterTypes: List<KClass<*>>,
		val mutex: Mutex
	);
}