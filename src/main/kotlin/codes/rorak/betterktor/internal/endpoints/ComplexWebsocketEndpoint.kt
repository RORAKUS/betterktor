package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.api.ComplexWebsocket
import codes.rorak.betterktor.internal.other.getKey
import codes.rorak.betterktor.internal.other.suspendCall
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import codes.rorak.betterktor.util.BetterKtorRuntimeError
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal class ComplexWebsocketEndpoint(cache: BetterKtorCache, val clazz: KClass<*>, val outerClass: EndpointClass?):
	BaseEndpoint(cache) {
	var onConect: FunctionObject? = null;
	var onClose: FunctionObject? = null;
	
	val onError = mutableListOf<FunctionObject>();
	val onMessage = mutableListOf<FunctionObject>();
	val onMessageSend = mutableListOf<FunctionObject>();
	
	val flowObjects = mutableMapOf<KClass<*>, KProperty1<*, MutableSharedFlow<*>>>();
	
	var mutex: Mutex? = null;
	
	private var ended = false;
	
	override fun register() {
		// authentication, path, instantiation, mutex, sending, receiving, catching, closing, intance setup
		cache.application.routing {
			// if auth is not null, use it
			CommonRegister.optionalAuth(auth, this) {
				route(path.render()) { webSocket { configureWebsocket(this) } };
			};
		};
	}
	
	private suspend fun configureWebsocket(session: DefaultWebSocketServerSession) {
		// create an instance of the class, process injected properties
		val instance = CommonRegister.handleInstance(classInfo!!, session, cache);
		// if it is a ComplexWebsocket class, set it up
		if (instance is ComplexWebsocket<*>) {
			// set lateinit variables
			instance.cache = cache;
			instance.sendMethod = generateSendMethod(session, instance);
		}
		// add the instance to the cache list
		cache.cwInstances.getOrPut(instance::class) { mutableListOf() }.add(instance);
		
		websocketLife(session, instance);
	}
	
	private suspend fun websocketLife(session: DefaultWebSocketServerSession, instance: Any) = runCatching {
		// start handler, stop handler, message receive handler, error handler + mutexed
		
		// inform the connect handler
		onConect?.let { f ->
			// get the parameters
			val parameters = f.parameterTypes.map { if (it == ApplicationCall::class) session.call else session };
			
			// call it with its mutex
			CommonRegister.optionalMutex(f.mutex) { f.function.suspendCall(instance, parameters) };
		};
		
		// launch a job collecting the messages from the flows
		session.launch {
			runCatching {
				flowObjects.forEach { (type, flowProp) ->
					// get the flow object
					val flow = flowProp.getter.call(instance);
					// collect and send the messages
					flow.collect {
						session.sendSerialized(it, TypeInfo(type));
					};
				};
			}.onFailure {
				// handle disconnection
				if (it !is CancellationException) throw it;
				onSessionEnd(session, instance);
				return@launch;
			};
		};
		
		// get the websocket deserializer
		val converter = cache.application.plugin(WebSockets).contentConverter!!;
		
		// receive the messages -> for every message...
		session.incoming.consumeEach { frame ->
			// consume only text
			if (frame !is Frame.Text) return@consumeEach;
			
			// try all possible types for deserialization get back to it -> type inheritance
			// go through all the message handlers, check for the type
			onMessage.forEach { onMessageHandler ->
				// try the deserialization, return the deserialized value on success
				val value = runCatching {
					converter.deserialize(Charsets.UTF_8, TypeInfo(onMessageHandler.distinctType!!), frame)!!;
				}.getOrElse { return@forEach; };
				
				// prepare the parameters for the message handler
				val parameters = onMessageHandler.parameterTypes
					.map { if (it == DefaultWebSocketServerSession::class) session else value };
				
				// call the handler with its mutex and the parameters
				CommonRegister.optionalMutex(onMessageHandler.mutex) {
					onMessageHandler.function.suspendCall(instance, parameters);
				};
				
				// skip other handlers
				return@consumeEach;
			};
			
			// todo not handled message
		};
		
	}.onFailure { err ->
		// find the correct error handler
		// fistly check for a direct type equality, then a supertype
		val errorHandler =
			onError.find { it.distinctType == it::class } ?: onError.find { it::class.isSubclassOf(it.distinctType!!) }
			// if no error handler found, just throw the error
			?: throw err;
		
		// process the parameters
		val parameters = errorHandler.parameterTypes
			.map { if (it == DefaultWebSocketServerSession::class) session else err };
		
		// call the function with its mutex
		CommonRegister.optionalMutex(errorHandler.mutex) { errorHandler.function.suspendCall(instance, parameters) };
	};
	
	private fun generateSendMethod(
		session: DefaultWebSocketServerSession,
		instance: ComplexWebsocket<*>
	): suspend (data: Any, type: KClass<*>) -> Unit =
		function@{ data, type ->
			// check if the session was not terminated
			if (!session.isActive) {
				onSessionEnd(session, instance);
				return@function;
			}
			
			// mutex wrap
			CommonRegister.optionalMutex(mutex) {
				// get the property of the flow for the current type
				// try the direct type first, then try a superclass of the to-be-sent object
				val flowProperty = flowObjects[type] ?: flowObjects.entries.find { type.isSubclassOf(it.key) }?.value
				// if no correct flow property was found, throw
				?: throw BetterKtorRuntimeError("No flow for type '$type' was found!");
				
				// get the instance of the flow
				@Suppress("UNCHECKED_CAST")
				val flow = flowProperty.getter.call(instance) as MutableSharedFlow<Any>;
				
				// get the message send handler for the correct type
				// use flowProperty.returnType.classifier instead of the parameter 'type', because a superclass could have been used
				val messageSendHandler = onMessageSend.find { it.distinctType == flowProperty.returnType.classifier };
				
				// save the data in a mutable property for a possible edit
				var data = data;
				
				// if the handler exists, use it and transform the value
				if (messageSendHandler != null) {
					// get the parameters for the handler
					val parameters = messageSendHandler.parameterTypes
						.map { if (it == DefaultWebSocketServerSession::class) session else data }.toMutableList();
					
					// if the mutex is the same as a parent one, you don't need it
					val sendHandlerMutex = if (messageSendHandler.mutex == mutex) null else messageSendHandler.mutex
					// get the new value with an optional mutex
					val newValue = CommonRegister.optionalMutex(sendHandlerMutex) {
						// call the handler, get the new value return
						// if the value is null, skip the sending completely
						messageSendHandler.function.suspendCall(instance, parameters)
							?: return@optionalMutex;
					};
					
					// edit the data
					data = newValue;
				}
				
				// send the data
				flow.emit(data);
			};
		};
	
	private suspend fun onSessionEnd(session: DefaultWebSocketServerSession, instance: Any) {
		// make sure this runs only once
		if (ended) return;
		ended = true;
		
		// if the onClose handler exists, call it
		onClose?.let { handler ->
			// get the parameters (it can only be the session)
			val parameters = handler.parameterTypes.map { session };
			
			// call the function with its mutex
			CommonRegister.optionalMutex(handler.mutex) { handler.function.suspendCall(instance, parameters) };
		};
		
		// remove the instance from the cache list
		cache.cwInstances[instance::class]!!.remove(instance);
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
		Flow properties: ${flowObjects.values.joinToString { "${it.name}: ${it.returnType}" }}
		================
	""".trimIndent();
}