package test.other.endpoints.api

import codes.rorak.betterktor.annotations.BKPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("relative")
class Wierd2: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Api Relative");
	}
}