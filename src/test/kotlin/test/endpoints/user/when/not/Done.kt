package test.endpoints.user.`when`.not

import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class Done: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("User When Not Done");
	}
	
	override suspend fun post(call: ApplicationCall) {
		throw Error();
	}
}