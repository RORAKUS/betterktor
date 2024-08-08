package different.pack.api.inner

import codes.rorak.betterktor.annotations.BKPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("/completely/different")
class Wierd: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Completely Different");
	}
}