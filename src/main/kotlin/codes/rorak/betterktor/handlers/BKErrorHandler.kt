package codes.rorak.betterktor.handlers

import io.ktor.server.application.*

/**
 * An interface for all error handlers. It is very simple and straight forward - you just override the
 * `onError` method.
 */
interface BKErrorHandler {
	suspend fun onError(call: ApplicationCall, cause: Throwable) {};
}