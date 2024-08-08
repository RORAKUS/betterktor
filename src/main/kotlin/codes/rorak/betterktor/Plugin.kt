package codes.rorak.betterktor

import codes.rorak.betterktor.handlers.BKErrorHandler
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.handlers.BKWebsocket
import codes.rorak.betterktor.internal.BKProcessor
import codes.rorak.betterktor.internal.BKProcessor.plusNotEmpty
import io.ktor.serialization.kotlinx.*
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.reflections.Reflections

/**
 * The plugin object
 */
val BKPlugin = createApplicationPlugin("BetterKtor", ::BKConfig) {
	val config = pluginConfig;
	
	// the package where all endpoints are
	val endpointPackage = (config.basePackage ?: getBasePackage()).plusNotEmpty(".") + config.endpointsPackage;
	
	val reflections = Reflections(endpointPackage);
	val routes = reflections.getSubTypesOf(BKRoute::class.java);
	val websockets = reflections.getSubTypesOf(BKWebsocket::class.java);
	val errorHandlers = reflections.getSubTypesOf(BKErrorHandler::class.java);
	
	if (config.installWebSockets && websockets.size > 0) application.install(WebSockets) {
		contentConverter = KotlinxWebsocketSerializationConverter(Json);
	};
	
	BKProcessor.processRoutes(application, config, routes, endpointPackage);
	BKProcessor.processWebsockets(application, config, websockets, endpointPackage);
	BKProcessor.processErrorHandlers(application, config, errorHandlers, endpointPackage);
};

private fun getBasePackage(): String {
	Thread.currentThread().stackTrace.forEach {
		if (it.className.contains("^(${{}::class.java.`package`.name}|java\\.lang|io\\.ktor)".toRegex())) return@forEach;
		
		return it.className.substringBeforeLast(".", "");
	};
	return "";
}