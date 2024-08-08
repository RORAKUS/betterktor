package simple.endpoints.user

import codes.rorak.betterktor.annotations.BKPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.response.*

@BKPath("relative/path")
class Wierd2: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("User Relative Path");
	}
}