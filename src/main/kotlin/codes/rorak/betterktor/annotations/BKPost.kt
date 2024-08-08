package codes.rorak.betterktor.annotations

/**
 * Sets a named route method's method to POST
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKPost;