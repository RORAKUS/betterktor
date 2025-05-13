package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.internal.other.getKey
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

internal class ComplexWebsocketEndpoint(cache: BetterKtorCache, val clazz: KClass<*>, val outerClass: EndpointClass?):
	BaseEndpoint(cache) {
	var onConect: FunctionObject? = null;
	var onClose: FunctionObject? = null;
	
	val onError = mutableListOf<FunctionObject>();
	val onMessage = mutableListOf<FunctionObject>();
	val onMessageSend = mutableListOf<FunctionObject>();
	
	val flowObjects = mutableListOf<KProperty1<*, MutableSharedFlow<*>>>();
	
	var mutex: Mutex? = null;
	
	override fun register() {
		TODO("Not yet implemented");
	}
	
	data class FunctionObject(
		val function: KFunction<*>,
		val parameterTypes: List<KClass<*>>,
		val distinctType: KClass<*>?,
		val mutex: Mutex?
	) {
		override fun toString() = "'${function.name}(${parameterTypes.joinToString { it.simpleName!! }})" +
				if (mutex == null) "'" else " (mutex = ${BetterKtor.mutexMap.getKey(mutex)})'";
	};
	
	override fun toString() = """
		=== Complex websocket endpoint '${clazz.simpleName}' ===
		${super.toString()}
		Mutex id: ${BetterKtor.mutexMap.getKey(mutex)}
		On connect handler: $onConect
		On close handler: $onClose
		On error handlers: ${onError.joinToString()}
		On message handlers: ${onMessage.joinToString()}
		On message send handlers: ${onMessageSend.joinToString()}
		Flow properties: ${flowObjects.joinToString { "${it.name}: ${it.returnType}" }}
		================
	""".trimIndent();
}