package codes.rorak.betterktor.annotations

/**
 * Sets a regex path for the route. If the path starts with '/', it is an absolute path,
 * which means it ignores all previous paths, including `config.rootPath`.
 * Supports all path patterns like `Routing` plugin does.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKRegexPath(val path: String);