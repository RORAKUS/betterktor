package test.endpoints

import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.response.*

class JustPost: BKRoute {
	override suspend fun post(call: ApplicationCall) {
		call.respondText("Just Post");
	}
}