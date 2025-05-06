package codes.rorak.betterktor

import codes.rorak.betterktor.annotations.Auth
import codes.rorak.betterktor.annotations.Method
import codes.rorak.betterktor.api.BetterKtor
import codes.rorak.betterktor.api.PageReturn
import codes.rorak.betterktor.api.TemplateReturn
import codes.rorak.betterktor.internal.other.Util
import codes.rorak.betterktor.internal.other.debug
import codes.rorak.betterktor.util.Casing
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.jte.*
import io.ktor.server.mustache.*
import io.ktor.server.pebble.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.velocity.*
import kotlinx.coroutines.sync.Mutex

/**
 * The config class for the BetterKtor plugin
 * @see BetterKtor
 */
class BetterKtorConfig {
	/**
	 * A set of all packages where BetterKtor searches for the endpoints
	 *
	 * Default: `[ "endpoints" ]`
	 */
	var endpointsPackages = mutableSetOf("endpoints");
	
	/**
	 * Provides easier way to set a package, that is used to search for the endpoints.
	 * Clears the property [endpointsPackages] and adds the value
	 */
	var endpointsPackage
		get() = endpointsPackages.first()
		set(v) {
			endpointsPackages = mutableSetOf(v);
		};
	
	
	/**
	 * A name for the default directory where to search for HTML pages in
	 * [ApplicationCall.respondPage] and [PageReturn]
	 *
	 * Default: `"pages"`
	 */
	var defaultPagesDirectory = "pages"
		set(v) {
			field = v;
			BetterKtor.defaultPagesDirectory = field;
		};
	
	/**
	 * A name for the default directory where to search for template pages in
	 * [TemplateReturn]
	 *
	 * Default: `"pages"`
	 */
	var defaultTemplatesDirectory = "templates";
	
	/**
	 * A name for the default authentication to use in [Auth] annotations
	 *
	 * Default: `"auth"`
	 * @see Auth
	 */
	var defaultAuthId = "auth";
	
	/**
	 * Wheter to use a [Mutex] for all endpoints.
	 * [BetterKtor.defaultMutex] will be used if `true`
	 */
	var useMutex = false;
	
	/**
	 * The default way for transforming the endpoint names
	 *
	 * Default: [Casing.kebabCase]
	 * @see Casing
	 */
	var defaultCasing = Casing.kebabCase;
	
	/**
	 * The default method to use in named routes and method routes
	 *
	 * Default: [HttpMethod.Post]
	 * @see Method
	 */
	var defaultNamedRouteMethod = HttpMethod.Post;
	
	
	/**
	 * The root package of your package, that contains the [endpointsPackage]
	 *
	 * Default: computed
	 */
	var basePackage = Util.fetchBasePackage();
	
	/**
	 * A method that is used in [TemplateReturn] for responding with templates.
	 * If null, BetterKtor will try to set it according to known plugins.
	 * If everything fails, [TemplateReturn] won't work.
	 *
	 * Default: `null`
	 */
	var templateRespondMethod: (suspend ApplicationCall.(path: String, parameters: Map<String, Any?>, status: HttpStatusCode) -> Unit)? =
		null;
	
	/**
	 * Root URL path for all endpoints
	 *
	 * Default: `""`
	 */
	var rootPath = ""
		set(value) {
			field = value.dropLastWhile { it == '/' };
		};
	
	/**
	 * Wheter the resolver operates in strict mode.
	 * In strict mode the only entities recognized are those, which
	 * implementa BetterKtor interface or are annotated with BetterKtor annotations -
	 * resolving by name is disabled.
	 *
	 * Default: `false`
	 */
	var strict = false;
	
	
	internal val naming = BetterKtorNamingConfig();
	
	/**
	 * Allows you to set names for name recognized endpoints or injections
	 */
	fun naming(initBlock: (BetterKtorNamingConfig.() -> Unit)) {
		naming.initBlock();
	}
	
	internal fun resolveTemplateRespondMethod(application: Application) {
		if (templateRespondMethod != null) return;
		
		debug("Resolving the template engine...");
		
		// when using FreeMaker
		runCatching { application.plugin(FreeMarker) }.onSuccess {
			templateRespondMethod = { name, params, status ->
				respond(status, FreeMarkerContent(name, params));
			};
			debug("Using the FreeMarker engine.");
			return;
		};
		// when using Velocity
		runCatching { application.plugin(Velocity) }.onSuccess {
			templateRespondMethod = { name, parameters, status ->
				respond(status, VelocityContent(name, parameters.nonNull()));
			};
			debug("Using the Velocity engine.");
			return;
		};
		// when using Mustache
		runCatching { application.plugin(Mustache) }.onSuccess {
			templateRespondMethod = { name, parameters, status ->
				respond(status, MustacheContent(name, parameters));
			};
			debug("Using the Mustache engine.");
			return;
		};
		// when using Thymeleaf
		runCatching { application.plugin(Thymeleaf) }.onSuccess {
			templateRespondMethod = { name, parameters, status ->
				respond(status, ThymeleafContent(name, parameters.nonNull()));
			};
			debug("Using the Thymeleaf engine.");
			return;
		};
		// when using Pebble
		runCatching { application.plugin(Pebble) }.onSuccess {
			templateRespondMethod = { name, parameters, status ->
				respond(status, PebbleContent(name, parameters.nonNull()));
			};
			debug("Using the Pebble engine.");
			return;
		};
		// when using JTE
		runCatching { application.plugin(Jte) }.onSuccess {
			templateRespondMethod = { name, parameters, status ->
				respond(status, JteContent(name, parameters));
			};
			debug("Using the JTE engine.");
			return;
		};
		
		debug("No template engine found!");
	}
	
	private fun Map<String, Any?>.nonNull() = map {
		it.key to (it.value ?: "null")
	}.toMap();
}

private const val NAME_CONTINUE_REGEX = "([A-Z1-9].*)?";

/**
 * Configuration for names of methods for the BetterKtor resolver
 *
 * @see BetterKtorConfig
 * @see BetterKtorConfig.naming
 */
class BetterKtorNamingConfig {
	/**
	 * Map of the regex names to their [HttpMethod] methods
	 */
	var httpMethods =
		HttpMethod.DefaultMethods.associateBy { "^${it.value.lowercase()}${NAME_CONTINUE_REGEX}$".toRegex() };
	
	/**
	 * The name regex for an endpoint accepting all methods
	 */
	var anyMethod = "^any$".toRegex();
	
	/**
	 * The name regex for a websocket endpoint
	 */
	var websocket = "^websocket${NAME_CONTINUE_REGEX}$".toRegex();
	
	/**
	 * The name regex for an SSE endpoint
	 */
	var sse = "^sse${NAME_CONTINUE_REGEX}$".toRegex();
	
	/**
	 * The name regex for an error handler
	 */
	var errorHandler = "^errorHandler${NAME_CONTINUE_REGEX}$".toRegex();
	
	/**
	 * The regex for ignored endpoints
	 */
	var ignored = "^(?:_.+|[Ii]gnore${NAME_CONTINUE_REGEX})$".toRegex();
	
	/**
	 * The name regex for a complex websocket
	 */
	var complexWebsocket = "^CW${NAME_CONTINUE_REGEX}$".toRegex();
	
	/**
	 * The regex for complex websocket method for handling new connections
	 */
	var complexWebsocketOnConnect = "^onConnection$".toRegex();
	
	/**
	 * The regex for complex websocket method for handling the connection end
	 */
	var complexWebsocketOnClose = "^onClose$".toRegex();
	
	/**
	 * The regex for complex websocket method for handling a new message
	 */
	var complexWebsocketOnMessage = "^onMessage$".toRegex();
	
	/**
	 * The regex for complex websocket method for handling errors
	 */
	var complexWebsocketOnError = "^onError$".toRegex();
	
	/**
	 * The regex for complex websocket property containing the message flow
	 */
	var complexWebsocketFlow = "^(?:(.+)F|f)low$".toRegex();
}