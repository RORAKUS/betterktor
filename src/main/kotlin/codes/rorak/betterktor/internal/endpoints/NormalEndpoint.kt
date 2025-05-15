package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.api.CallReturn
import codes.rorak.betterktor.internal.other.dropLines
import codes.rorak.betterktor.internal.other.getKey
import codes.rorak.betterktor.internal.other.suspendCall
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf

internal class NormalEndpoint(cache: BetterKtorCache, f: KFunction<*>):
	FunctionEndpoint(cache, f) {
	
	lateinit var returnType: KClass<*>;
	lateinit var httpMethod: HttpMethod;
	var mutex: Mutex? = null;
	
	override fun register() {
		// mutex, returning, parameters, instantiation, property injection
		
		cache.application.routing {
			// if auth is not null, use it
			CommonRegister.optionalAuth(auth, this) {
				// register the route with the specified path and http method
				route(path.render(), httpMethod) {
					handle {
						// get the instance if the function has a parent class
						//  (it also injects the properties)
						val instance = classInfo?.let { CommonRegister.handleInstance(it, call, cache) };
						// receive the parameters
						val parameters = parameterTypes.map { it.value.getter!!.invoke(call) }.toMutableList();
						
						// if mutex is required, use it. Call the provided function, get the return value
						val returnValue =
							CommonRegister.optionalMutex(mutex) { function.suspendCall(instance, parameters) };
						
						// if the call was already answered, return
						if (call.isHandled) return@handle;
						
						// if the return value type does not match the set type, throw
						if (returnValue == null || !returnValue::class.isSubclassOf(returnType))
							throw IllegalStateException();
						
						// respond, according to the return value
						if (returnValue is CallReturn) returnValue.respond(call, cache);
						else call.respond(returnValue, TypeInfo(returnType));
					};
				};
			};
		};
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