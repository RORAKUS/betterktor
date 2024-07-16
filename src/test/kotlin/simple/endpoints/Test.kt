package simple.endpoints

import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class Test: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Test");
	}
	
	override suspend fun post(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Test Post");
	}
}