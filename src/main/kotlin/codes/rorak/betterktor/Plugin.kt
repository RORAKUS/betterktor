package codes.rorak.betterktor

import codes.rorak.betterktor.handlers.BKErrorHandler
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.handlers.BKWebsocket
import codes.rorak.betterktor.internal.BKProcessor
import codes.rorak.betterktor.internal.BKProcessor.plusNotEmpty
import codes.rorak.betterktor.util.BKConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.websocket.*
import org.reflections.Reflections
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

/**
 * The plugin object
 */
val BKPlugin = createApplicationPlugin("BetterKtor", ::BKConfig) {
	val config = pluginConfig;
	
	// install plugins
	config.authConfig?.let { application.install(Authentication, it); };
	config.websocketConfig?.let { application.install(WebSockets, it) };
	
	// the package where all endpoints are
	val endpointPackage = (config.basePackage ?: getBasePackage()).plusNotEmpty(".") + config.endpointsPackage;
	
	val reflections = Reflections(endpointPackage);
	val routes = reflections.getSubTypesOf(BKRoute::class.java);
	val websockets = reflections.getSubTypesOf(BKWebsocket::class.java);
	val errorHandlers = reflections.getSubTypesOf(BKErrorHandler::class.java);
	
	BKProcessor.processRoutes(application, config, routes, endpointPackage);
	BKProcessor.processWebsockets(application, config, websockets, endpointPackage);
	BKProcessor.processErrorHandlers(application, config, errorHandlers, endpointPackage);
};

private fun getBasePackage(): String {
	Thread.currentThread().stackTrace.forEach {
		if (it.className.contains("^(${{}::class.java.`package`.name}|java\\.lang|io\\.ktor|jdk|executors)".toRegex())) return@forEach;
		
		return it.className.substringBeforeLast(".", "");
	};
	return "";
}

fun KFunction<*>.isIn(col: Collection<KFunction<*>>) = col.any { fn ->
	name == fn.name && parameters.size == fn.parameters.size
			&& fn.parameters.map(KParameter::type).drop(1) == parameters.map(KParameter::type).drop(1);
}