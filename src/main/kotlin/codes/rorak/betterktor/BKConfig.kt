package codes.rorak.betterktor

/**
 * The config class for the `BKPlugin`
 */
class BKConfig {
	/**
	 * The name of the package containing all endpoints.
	 * Default: "endpoints"
	 */
	var packageName = "endpoints";
	
	/**
	 * The name of the base package, in which the `packageName` package is.
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
}