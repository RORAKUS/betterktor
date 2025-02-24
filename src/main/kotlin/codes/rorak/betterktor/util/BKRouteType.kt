package codes.rorak.betterktor.util

import codes.rorak.betterktor.handlers.BKErrorHandler
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.handlers.BKWebsocket
import codes.rorak.betterktor.isIn
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions

enum class BKRouteType(private val clazz: KClass<*>) {
	ROUTE(BKRoute::class),
	WEBSOCKET(BKWebsocket::class),
	ERROR_HANDLER(BKErrorHandler::class),
	NONE(Nothing::class);
	
	fun contains(method: KFunction<*>): Boolean {
		if (this == NONE) return false;
		
		val collection = clazz.declaredFunctions;
		return method.isIn(collection);
	}
	
	fun restDoesNotContain(method: KFunction<*>) = values().none { it != this && it.contains(method) };
}