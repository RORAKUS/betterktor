package codes.rorak.betterktor.annotations

import io.ktor.server.auth.*

/**
 * Makes the class or the function require authentication.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BKAuth(
	vararg val providers: String,
	val strategy: AuthenticationStrategy = AuthenticationStrategy.Required
);