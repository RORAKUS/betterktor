package simple.endpoints.user

import codes.rorak.betterktor.BKPath
import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("/completely/different/path")
class Wierd: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Completely Different Path");
	}
}