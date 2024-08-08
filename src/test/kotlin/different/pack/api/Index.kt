package different.pack.api

import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class Index: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Api");
	}
}