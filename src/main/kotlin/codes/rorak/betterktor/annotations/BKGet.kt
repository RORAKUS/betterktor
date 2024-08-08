package codes.rorak.betterktor.annotations

/**
 * Sets a named route method's method to GET
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKGet;