package codes.rorak.betterktor.util

import io.ktor.server.auth.*

/**
 * The config class for the `codes.rorak.betterktor.getBKPlugin`
 */
class BKConfig {
	/**
	 * The name of the package containing all endpoints.
	 * Default: "endpoints"
	 */
	var endpointsPackage = "endpoints";
	
	/**
	 * The name of the base package, in which the `endpointsPackage` package is.
	 * Default: computed using the call stack. It will be the path of the package where is `install()` called from.
	 * Can be really imprecise, so if BK does not work, try setting this :)
	 */
	var basePackage: String? = null;
	
	/**
	 * A function used for name transforming. All frequently used cases are in class `BKTransform`
	 * Default: `BKTransform::kebabCase`
	 */
	var casing = BKTransform::kebabCase;
	
	/**
	 * A root HTTP path for all endpoints without the last "/".
	 * Default: ""
	 */
	var rootPath = ""
		set(value) {
			field = value.dropLastWhile { it == '/' };
		};
	
	/**
	 * Whether to install websockets if needed. Websockets will not work if this is set false, and you don't
	 * install them yourself!
	 * Default: true
	 */
	var installWebSockets = true;
	
	/**
	 * The default HTTP method for named route methods
	 * Default: POST
	 */
	var defaultNamedRouteMethod = BKHttpMethod.POST;
	
	/**
	 * The configuration for the authentication plugin
	 */
	fun configureAuthentication(config: AuthenticationConfig.() -> Unit) {
		authConfig = config; }
	
	internal var authConfig: (AuthenticationConfig.() -> Unit)? = null;
}