package codes.rorak.betterktor

import codes.rorak.betterktor.internal.other.debug
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import codes.rorak.betterktor.internal.resolver.BetterKtorResolver
import io.ktor.server.application.*

// todo advanced analytics system

/**
 * Main plugin
 *
 * Usage: `install(BetterKtor) { /* config */ }`
 * @see BetterKtorConfig
 */
val BetterKtor = createApplicationPlugin("BetterKtor", ::BetterKtorConfig) {
	debug("Installing...");
	
	// resolve config template respond method
	pluginConfig.resolveTemplateRespondMethod(application);
	
	// create the cache object
	val cache = BetterKtorCache(pluginConfig, application);
	// create the resolver
	val resolver = BetterKtorResolver(cache);
	// find all the endpoints and register them
	resolver.resolve();
	resolver.register();
	
	debug("Successfully installed!");
};

interface Test {
	fun a();
}

fun main() {
	println(Test::a.isOpen);
}