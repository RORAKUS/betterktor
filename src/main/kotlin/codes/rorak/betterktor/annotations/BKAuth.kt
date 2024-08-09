package codes.rorak.betterktor.annotations

/**
 * Makes the class or the function require authentication.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKAuth(val name: String);