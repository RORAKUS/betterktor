package test.endpoints.number

import codes.rorak.betterktor.handlers.BKErrorHandler
import io.ktor.server.application.*
import io.ktor.server.response.*

class Errors: BKErrorHandler {
	override suspend fun onError(call: ApplicationCall, cause: Throwable) {
		call.respondText("Error caught in number");
	}
}