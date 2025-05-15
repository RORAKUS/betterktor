package codes.rorak.betterktor.internal.endpoints

import codes.rorak.betterktor.internal.other.dropLines
import codes.rorak.betterktor.internal.other.suspendCall
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import kotlinx.coroutines.sync.Mutex
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

internal class ErrorHandlerEndpoint(cache: BetterKtorCache, f: KFunction<*>): // fixme error inheritance
	FunctionEndpoint(cache, f) {
	lateinit var errorType: KClass<*>;
	var mutex: Mutex? = null;
	
	override fun register() {
		cache.statusPage {
			exception<Throwable> { call, cause ->
				// if the type is not correct, return
				if (cause::class != errorType) return@exception;
				
				// get the instance if the function has a parent class
				//  (it also injects the properties)
				val instance = classInfo?.let { CommonRegister.handleInstance(it, call, cache) };
				// receive the parameters, check for the error parameter
				val parameters = parameterTypes.map {
					if (it.value.isError) cause
					else it.value.getter!!.invoke(call)
				}.toMutableList();
				
				// if mutex is required, use it and call the provided function
				CommonRegister.optionalMutex(mutex) { function.suspendCall(instance, parameters) };
			};
		};
	}
	
	override fun toString() = """
		${super.toString().dropLines(1)}
		Error type: ${errorType.simpleName}
		================
	""".trimIndent();
}