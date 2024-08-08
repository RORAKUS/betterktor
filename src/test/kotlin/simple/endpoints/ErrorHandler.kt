package simple.endpoints

import codes.rorak.betterktor.handlers.BKErrorHandler
import io.ktor.server.application.*
import io.ktor.server.response.*

class ErrorHandler: BKErrorHandler {
	override suspend fun onError(call: ApplicationCall, cause: Throwable) {
		call.respondText("Error caught in /!")
	}
	
	@Suppress("UNUSED_PARAMETER")
	suspend fun test(call: ApplicationCall, cause: Throwable) {
		call.respondText("Error caught in /test!")
	}
}