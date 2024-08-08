package codes.rorak.betterktor.annotations

/**
 * An annotation that can be used on `BKRoute`. Specifies a custom path for the route.
 * Supports all path patterns like `Routing` plugin does.
 * If it starts with '/', it is an absolute path, ignoring even the rootPath. If it doesn't, it is a relative path
 * to its package.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKPath(val path: String = "");